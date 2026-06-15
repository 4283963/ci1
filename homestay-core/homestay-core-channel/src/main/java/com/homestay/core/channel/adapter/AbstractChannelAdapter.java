package com.homestay.core.channel.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.core.channel.model.ChannelOrderDTO;
import com.homestay.core.channel.model.ChannelWebhookEvent;
import com.homestay.core.common.exception.BusinessException;
import com.homestay.core.model.entity.ChannelAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
public abstract class AbstractChannelAdapter implements ChannelAdapter {

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;

    protected AbstractChannelAdapter() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    protected <T> T doPost(String url, Object body, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, httpHeaders);
            ResponseEntity<T> resp = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return resp.getBody();
        } catch (Exception e) {
            log.error("渠道API调用失败: channel={}, url={}", getChannelCode(), url, e);
            throw BusinessException.of("渠道API调用失败: " + e.getMessage());
        }
    }

    protected <T> T doGet(String url, Map<String, String> queryParams, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            StringBuilder urlBuilder = new StringBuilder(url);
            if (queryParams != null && !queryParams.isEmpty()) {
                urlBuilder.append("?");
                queryParams.forEach((k, v) -> urlBuilder.append(k).append("=").append(v).append("&"));
                urlBuilder.deleteCharAt(urlBuilder.length() - 1);
            }
            ResponseEntity<T> resp = restTemplate.exchange(urlBuilder.toString(), HttpMethod.GET, entity, responseType);
            return resp.getBody();
        } catch (Exception e) {
            log.error("渠道API调用失败: channel={}, url={}", getChannelCode(), url, e);
            throw BusinessException.of("渠道API调用失败: " + e.getMessage());
        }
    }

    protected Map<String, Object> parseJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON解析失败", e);
            return Map.of();
        }
    }

    protected Map<String, Object> getExtraConfig(ChannelAccount account) {
        if (account.getExtraConfig() == null || account.getExtraConfig().isEmpty()) {
            return Map.of();
        }
        return parseJsonToMap(account.getExtraConfig());
    }

    protected abstract Map<String, String> buildAuthHeaders(ChannelAccount account);

    @Override
    public ChannelResult<ChannelOrderDTO> parseWebhookEvent(ChannelWebhookEvent event, ChannelAccount account) {
        log.info("[{}] 解析Webhook事件: type={}", getChannelCode(), event.getEventType());
        if (!verifyWebhookSignature(event, account)) {
            return ChannelResult.fail("SIGN_INVALID", "Webhook签名校验失败");
        }
        try {
            ChannelOrderDTO order = doParseWebhook(event);
            return ChannelResult.ok(order);
        } catch (Exception e) {
            log.error("[{}] Webhook事件解析失败", getChannelCode(), e);
            return ChannelResult.fail("PARSE_ERROR", e.getMessage());
        }
    }

    protected abstract ChannelOrderDTO doParseWebhook(ChannelWebhookEvent event) throws Exception;

    protected void logApi(String method, String url, Object request, Object response) {
        log.debug("[{}] API {} {}: request={}, response={}", getChannelCode(), method, url, request, response);
    }
}
