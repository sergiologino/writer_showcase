package io.altacod.publisher.integration;

import io.altacod.publisher.api.dto.AiInvokeResponse;

public interface IntegrationAiClient {

    AiInvokeResponse send(NoteappAiProcessRequest request);
}
