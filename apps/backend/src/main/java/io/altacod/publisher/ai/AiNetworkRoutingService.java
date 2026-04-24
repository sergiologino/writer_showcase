package io.altacod.publisher.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Глобальный порядок имён нейросетей по {@code requestType} (как в noteapp-ai-integration) для fallback.
 */
@Service
public class AiNetworkRoutingService {

    public static final long SINGLETON_ID = 1L;

    private final AppAiRoutingRepository repository;
    private final ObjectMapper objectMapper;

    public AiNetworkRoutingService(AppAiRoutingRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<String> orderedNamesForRequestType(String requestType) {
        String r = requestType == null || requestType.isBlank() ? "chat" : requestType.trim();
        Map<String, List<String>> all = readAll();
        List<String> list = all.get(r);
        if (list == null) {
            return List.of();
        }
        return list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getRouting() {
        return readAll();
    }

    @Transactional
    public void saveRouting(Map<String, List<String>> routing) {
        if (routing == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "routing is required");
        }
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (var e : routing.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) {
                continue;
            }
            List<String> names = e.getValue() == null
                    ? new ArrayList<>()
                    : e.getValue().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toCollection(ArrayList::new));
            normalized.put(e.getKey().trim(), names);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(normalized);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid routing");
        }
        AppAiRoutingEntity row = repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "app_ai_routing missing"));
        row.setRoutingJson(json);
        row.setUpdatedAt(Instant.now());
        repository.save(row);
    }

    private Map<String, List<String>> readAll() {
        AppAiRoutingEntity row = repository.findById(SINGLETON_ID).orElse(null);
        if (row == null || row.getRoutingJson() == null || row.getRoutingJson().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(
                    row.getRoutingJson(),
                    new TypeReference<>() {
                    }
            );
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
