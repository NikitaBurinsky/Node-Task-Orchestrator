package nto.web.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrorResponseFactory {

    private ApiErrorResponseFactory() {
    }

    public static Map<String, Object> buildBody(HttpStatus status, String error, Object details) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        if (details != null) {
            response.put("details", details);
        }
        return response;
    }

    public static void writeResponse(HttpServletResponse response,
                                     ObjectMapper objectMapper,
                                     HttpStatus status,
                                     String error,
                                     Object details) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), buildBody(status, error, details));
    }
}
