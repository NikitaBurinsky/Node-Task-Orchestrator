package nto.web;

import nto.application.interfaces.services.ServerGroupService;
import nto.web.advice.GlobalExceptionHandler;
import nto.web.controllers.ServerGroupController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerGroupControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ServerGroupService groupService = mock(ServerGroupService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ServerGroupController(groupService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @Test
    void createBulkShouldReturn400WhenNameIsBlank() throws Exception {
        String body = """
            {
              "name": "",
              "servers": [
                {
                  "hostname": "srv-1",
                  "ipAddress": "10.0.0.1",
                  "port": 22,
                  "username": "root",
                  "password": "pw"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/groups/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.name").value("Name is required"));
    }

    @Test
    void createBulkShouldReturn400WhenServersListIsEmpty() throws Exception {
        String body = """
            {
              "name": "bulk-group",
              "servers": []
            }
            """;

        mockMvc.perform(post("/api/groups/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.servers").value("Server list cannot be empty"));
    }
}
