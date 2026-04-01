package nto.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nto.infrastructure.security.RestAccessDeniedHandler;
import nto.infrastructure.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestSecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void authenticationEntryPointShouldWriteUnifiedUnauthorizedResponse() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
            new BadCredentialsException("Bad credentials"));

        Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(),
            new TypeReference<>() {
            });

        assertEquals(401, response.getStatus());
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
    }

    @Test
    void accessDeniedHandlerShouldWriteUnifiedForbiddenResponse() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response,
            new AccessDeniedException("Forbidden area"));

        Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(),
            new TypeReference<>() {
            });

        assertEquals(403, response.getStatus());
        assertEquals(403, body.get("status"));
        assertEquals("Forbidden area", body.get("error"));
    }
}
