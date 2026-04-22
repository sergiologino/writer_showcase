package io.altacod.publisher.channel.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Проверяет формат исходящего запроса к MAX Bot API (без полного контекста Spring).
 */
class MaxMessengerHttpContractTest {

    @Test
    void postMessageUsesBearerAuthAndJsonBody() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://platform-api.max.ru");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String responseJson = """
                {"message":{"id":424242,"chat_id":1}}
                """;
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("text", "hello");
        body.put("disable_link_preview", false);
        String bodyStr = objectMapper.writeValueAsString(body);

        server.expect(requestTo("https://platform-api.max.ru/messages?chat_id=1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token-xyz"))
                .andExpect(content().string(containsString("\"text\":\"hello\"")))
                .andExpect(content().string(containsString("\"disable_link_preview\":false")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        RestClient client = builder.build();
        String raw = client.post()
                .uri("/messages?chat_id=1")
                .header("Authorization", "Bearer test-token-xyz")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyStr)
                .retrieve()
                .body(String.class);

        server.verify();
        org.junit.jupiter.api.Assertions.assertTrue(raw.contains("424242"));
    }
}
