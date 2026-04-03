package nto.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import nto.core.utils.exceptions.BadRequestException;
import nto.infrastructure.logging.FailedRequestLoggingFilter;
import nto.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FailedRequestLoggingFilterTest {

    private MockMvc mockMvc;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(FailedRequestLoggingFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new FailedRequestLoggingFilter())
            .build();
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(FailedRequestLoggingFilter.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void shouldLogClientErrorsAtWarnLevel() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
            .andExpect(status().isBadRequest());

        List<ILoggingEvent> events = logAppender.list;
        assertEquals(1, events.size());
        assertEquals(Level.WARN, events.getFirst().getLevel());
        assertTrue(events.getFirst().getFormattedMessage().contains("status=400"));
        assertTrue(events.getFirst().getFormattedMessage().contains("method=GET"));
        assertTrue(events.getFirst().getFormattedMessage().contains("path=/test/bad-request"));
    }

    @Test
    void shouldLogServerErrorsAtErrorLevel() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
            .andExpect(status().isInternalServerError());

        List<ILoggingEvent> events = logAppender.list;
        assertEquals(1, events.size());
        assertEquals(Level.ERROR, events.getFirst().getLevel());
        assertTrue(events.getFirst().getFormattedMessage().contains("status=500"));
        assertTrue(events.getFirst().getFormattedMessage().contains("path=/test/internal-error"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BadRequestException("Bad input");
        }

        @GetMapping("/test/internal-error")
        void internalError() {
            throw new IllegalStateException("Boom");
        }
    }
}
