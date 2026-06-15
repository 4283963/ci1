package com.homestay.core.lock.impl;

import com.homestay.core.lock.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = buildLockKey(joinPoint, distributedLock);
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.unit());
        if (!locked) {
            log.warn("获取分布式锁失败: lockKey={}", lockKey);
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        try {
            log.debug("获取分布式锁成功: lockKey={}", lockKey);
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁成功: lockKey={}", lockKey);
            }
        }
    }

    private String buildLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        String dynamicKey = distributedLock.key();
        if (!dynamicKey.isEmpty() && paramNames != null) {
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            Expression expression = spelParser.parseExpression(dynamicKey);
            String value = expression.getValue(context, String.class);
            if (value != null && !value.isEmpty()) {
                dynamicKey = value;
            }
        }

        return String.format("%s:%s", distributedLock.prefix(), dynamicKey.isEmpty() ? method.getName() : dynamicKey);
    }
}
