package com.homestay.core.common.exception;

import com.homestay.core.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(InventoryInsufficientException.class)
    public R<Void> handleInventoryException(InventoryInsufficientException e) {
        log.warn("库存异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({DeadlockLoserDataAccessException.class, CannotAcquireLockException.class})
    public R<Void> handleDeadlockException(Exception e) {
        log.error("数据库锁冲突/死锁异常", e);
        return R.fail(50001, "系统繁忙，请稍后重试");
    }

    @ExceptionHandler({QueryTimeoutException.class, TimeoutException.class})
    public R<Void> handleTimeoutException(Exception e) {
        log.error("查询超时", e);
        return R.fail(50002, "请求超时，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统内部错误");
    }
}
