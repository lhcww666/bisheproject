package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 签发和校验短期 Agent 服务上下文。
 */
@Service
public class AgentContextService
{
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final int CONTEXT_VERSION = 1;

    private static final int MAX_TOKEN_LENGTH = 8192;

    private static final Set<String> ROLES = Set.of("STUDENT", "TEACHER", "ADMIN", "USER");

    private final AgentProperties properties;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    public AgentContextService(AgentProperties properties, ObjectMapper objectMapper)
    {
        this(properties, objectMapper, Clock.systemUTC());
    }

    AgentContextService(AgentProperties properties, ObjectMapper objectMapper, Clock clock)
    {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String issue(Long userId, String role, Long sessionId, String traceId, List<String> allowedTools)
    {
        String secret = requireSecret();
        String normalizedRole = validateRole(role);
        if (userId == null || sessionId == null || StringUtils.isEmpty(traceId))
        {
            throw new ServiceException("Agent 上下文缺少用户、会话或追踪信息");
        }
        long issuedAt = clock.instant().toEpochMilli();
        long expiresAt = issuedAt + Math.max(properties.getContextTtlSeconds(), 1) * 1000;
        AgentContextClaims claims = new AgentContextClaims(
                CONTEXT_VERSION,
                userId,
                normalizedRole,
                sessionId,
                traceId,
                allowedTools == null ? List.of() : List.copyOf(allowedTools),
                issuedAt,
                expiresAt);
        try
        {
            byte[] payload = objectMapper.writeValueAsBytes(claims);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            return encodedPayload + "." + sign(encodedPayload, secret);
        }
        catch (Exception exception)
        {
            throw new ServiceException("签发 Agent 服务上下文失败");
        }
    }

    public AgentContextClaims verify(String token)
    {
        String secret = requireSecret();
        if (StringUtils.isEmpty(token) || token.length() > MAX_TOKEN_LENGTH)
        {
            throw invalidContext();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty())
        {
            throw invalidContext();
        }

        byte[] actualSignature = parts[1].getBytes(StandardCharsets.US_ASCII);
        byte[] expectedSignature = sign(parts[0], secret).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(actualSignature, expectedSignature))
        {
            throw invalidContext();
        }

        try
        {
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            AgentContextClaims claims = objectMapper.readValue(payload, AgentContextClaims.class);
            if (claims.getVersion() != CONTEXT_VERSION
                    || claims.getUserId() == null
                    || claims.getSessionId() == null
                    || StringUtils.isEmpty(claims.getTraceId())
                    || claims.getExpiresAt() <= clock.instant().toEpochMilli())
            {
                throw invalidContext();
            }
            validateRole(claims.getRole());
            return claims;
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw invalidContext();
        }
    }

    public AgentContextClaims verify(String token, String requestId)
    {
        AgentContextClaims claims = verify(token);
        if (StringUtils.isEmpty(requestId) || !requestId.equals(claims.getTraceId()))
        {
            throw invalidContext();
        }
        return claims;
    }

    private String requireSecret()
    {
        String secret = properties.getContextSecret();
        if (StringUtils.isEmpty(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32)
        {
            throw new ServiceException("Agent 服务上下文密钥未配置或长度不足32字节");
        }
        return secret;
    }

    private String validateRole(String role)
    {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalizedRole))
        {
            throw new ServiceException("Agent 角色不合法");
        }
        return normalizedRole;
    }

    private String sign(String payload, String secret)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        }
        catch (Exception exception)
        {
            throw new ServiceException("Agent 服务上下文签名失败");
        }
    }

    private ServiceException invalidContext()
    {
        return new ServiceException("Agent 服务上下文无效或已过期", HttpStatus.UNAUTHORIZED);
    }
}
