package com.homestay.sync.controller;

import com.homestay.sync.webhook.WebhookEventParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookEventParser webhookEventParser;

    private static final Map<String, Long> CHANNEL_ID_MAP = new HashMap<>();

    static {
        CHANNEL_ID_MAP.put("CTRIP", 1L);
        CHANNEL_ID_MAP.put("MEITUAN", 2L);
        CHANNEL_ID_MAP.put("FLIGGY", 3L);
        CHANNEL_ID_MAP.put("AIRBNB", 4L);
        CHANNEL_ID_MAP.put("DIRECT", 5L);
    }

    @PostMapping("/{channelCode}")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @PathVariable String channelCode,
            @RequestHeader(value = "X-Event-Type", required = false) String eventTypeHeader,
            @RequestParam(value = "event", required = false) String eventParam,
            HttpServletRequest request) {

        String rawBody = readBody(request);
        String eventType = eventTypeHeader != null ? eventTypeHeader :
                (eventParam != null ? eventParam : inferEventType(rawBody));

        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.put(name, request.getHeader(name)));

        Long channelId = CHANNEL_ID_MAP.getOrDefault(channelCode.toUpperCase(), 0L);

        webhookEventParser.parseAndProcess(channelCode.toUpperCase(), channelId,
                eventType, rawBody, headers);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "received");
        resp.put("eventId", UUID.randomUUID().toString());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{channelCode}/verify")
    public ResponseEntity<String> verifyWebhook(
            @PathVariable String channelCode,
            @RequestParam Map<String, String> params) {
        log.info("[{}] Webhook验证请求: params={}", channelCode, params);
        String challenge = params.getOrDefault("echostr",
                params.getOrDefault("challenge", params.getOrDefault("verify_token", "OK")));
        return ResponseEntity.ok(challenge);
    }

    private String readBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("读取Webhook Body失败", e);
            return "{}";
        }
    }

    private String inferEventType(String body) {
        if (body.contains("cancel") || body.contains("取消")) return "ORDER_CANCEL";
        if (body.contains("modify") || body.contains("update") || body.contains("修改")) return "ORDER_MODIFY";
        return "ORDER_CREATE";
    }
}
