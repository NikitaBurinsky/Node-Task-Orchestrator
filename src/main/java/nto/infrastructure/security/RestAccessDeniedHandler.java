package nto.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nto.core.utils.ErrorMessages;
import nto.web.advice.ApiErrorResponseFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String message = accessDeniedException.getMessage();
        if (message == null || message.isBlank()) {
            message = ErrorMessages.ACCESS_DENIED.getMessage();
        }
        ApiErrorResponseFactory.writeResponse(response, objectMapper, HttpStatus.FORBIDDEN,
            message, null);
    }
}
