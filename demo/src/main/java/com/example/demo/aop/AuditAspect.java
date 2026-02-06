package com.example.demo.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogService auditLogService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.example.demo.aop.Auditable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String oldValue = toJsonSafely(joinPoint.getArgs());
        Long entityId = resolveEntityId(auditable, joinPoint.getArgs());
        Long userId = resolveUserId();
        String ipAddress = resolveIpAddress();

        Object result = null;
        Throwable thrown = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            thrown = ex;
            throw ex;
        } finally {
            String newValue = result != null ? toJsonSafely(result) : null;
            if (entityId == null && auditable.useResultEntityId() && result != null) {
                entityId = extractIdFromResult(result);
            }
            String message = thrown == null ? "OK" : "ERROR: " + thrown.getClass().getSimpleName();
            auditLogService.logDetailed(
                auditable.action(),
                auditable.entityType(),
                entityId,
                userId,
                oldValue,
                newValue,
                ipAddress,
                message
            );
        }
    }

    private String toJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit value", ex);
            return String.valueOf(value);
        }
    }

    private Long resolveEntityId(Auditable auditable, Object[] args) {
        int index = auditable.entityIdParamIndex();
        if (index >= 0 && args != null && args.length > index && args[index] != null) {
            Object value = args[index];
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        return null;
    }

    private Long extractIdFromResult(Object result) {
        try {
            Method getter = result.getClass().getMethod("getId");
            Object id = getter.invoke(result);
            if (id instanceof Long) {
                return (Long) id;
            }
            if (id instanceof Number) {
                return ((Number) id).longValue();
            }
        } catch (Exception ex) {
            log.debug("No getId() on result for audit logging");
        }
        return null;
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        AppUser user = userRepository.findByUsername(auth.getName()).orElse(null);
        return user != null ? user.getId() : null;
    }

    private String resolveIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
