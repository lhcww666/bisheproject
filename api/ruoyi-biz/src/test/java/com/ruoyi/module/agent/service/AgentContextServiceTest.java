package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentContextServiceTest
{
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static final Instant NOW = Instant.parse("2026-11-01T05:00:00Z");

    @Test
    void issueAndVerifyContext()
    {
        AgentContextService service = serviceAt(NOW);

        String token = service.issue(7L, "student", 101L, "trace-1", List.of("queryMyReservations"));
        AgentContextClaims claims = service.verify(token);

        assertEquals(7L, claims.getUserId());
        assertEquals("STUDENT", claims.getRole());
        assertEquals(101L, claims.getSessionId());
        assertEquals("trace-1", claims.getTraceId());
        assertEquals(List.of("queryMyReservations"), claims.getAllowedTools());
    }

    @Test
    void rejectTamperedContext()
    {
        AgentContextService service = serviceAt(NOW);
        String token = service.issue(7L, "student", 101L, "trace-1", List.of());
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThrows(ServiceException.class, () -> service.verify(tampered));
    }

    @Test
    void rejectMismatchedRequestId()
    {
        AgentContextService service = serviceAt(NOW);
        String token = service.issue(7L, "student", 101L, "trace-1", List.of());

        assertThrows(ServiceException.class, () -> service.verify(token, "trace-2"));
    }

    @Test
    void rejectExpiredContext()
    {
        AgentContextService issuer = serviceAt(NOW);
        String token = issuer.issue(7L, "student", 101L, "trace-1", List.of());
        AgentContextService verifier = serviceAt(NOW.plusSeconds(61));

        assertThrows(ServiceException.class, () -> verifier.verify(token));
    }

    @Test
    void rejectMissingSecret()
    {
        AgentProperties properties = new AgentProperties();
        AgentContextService service = new AgentContextService(
                properties,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(
                ServiceException.class,
                () -> service.issue(7L, "student", 101L, "trace-1", List.of()));
    }

    private AgentContextService serviceAt(Instant instant)
    {
        AgentProperties properties = new AgentProperties();
        properties.setContextSecret(SECRET);
        properties.setContextTtlSeconds(60);
        return new AgentContextService(
                properties,
                new ObjectMapper(),
                Clock.fixed(instant, ZoneOffset.UTC));
    }
}
