package io.altacod.publisher.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "YANDEX_CLIENT_ID=test-client",
        "YANDEX_CLIENT_SECRET=test-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiUnauthorizedWithOauthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiUnauthorizedDoesNotRedirectToLoginWhenOauthIsEnabled() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }
}
