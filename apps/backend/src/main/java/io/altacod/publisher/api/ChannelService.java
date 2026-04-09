package io.altacod.publisher.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.altacod.publisher.api.dto.ChannelResponse;
import io.altacod.publisher.api.dto.ChannelUpsertPayload;
import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.channel.WorkspaceChannelEntity;
import io.altacod.publisher.channel.WorkspaceChannelRepository;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;

@Service
public class ChannelService {

    private final WorkspaceChannelRepository channelRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public ChannelService(
            WorkspaceChannelRepository channelRepository,
            WorkspaceRepository workspaceRepository,
            ObjectMapper objectMapper
    ) {
        this.channelRepository = channelRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> list(Long workspaceId) {
        return channelRepository.findByWorkspaceIdOrderByChannelTypeAsc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChannelResponse upsert(Long workspaceId, ChannelType type, ChannelUpsertPayload payload) {
        var ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        validateJson(payload.configJson());
        Instant now = Instant.now();
        var existing = channelRepository.findByWorkspaceIdAndChannelType(workspaceId, type);
        String baseJson = existing.map(WorkspaceChannelEntity::getConfigJson).orElse("{}");
        if (baseJson.isBlank()) {
            baseJson = "{}";
        }
        String mergedConfig = mergeConfigJson(baseJson, payload.configJson());
        if (existing.isEmpty()) {
            var entity = new WorkspaceChannelEntity(
                    ws,
                    type,
                    Boolean.TRUE.equals(payload.enabled()),
                    blankToNull(payload.label()),
                    mergedConfig,
                    now
            );
            channelRepository.save(entity);
            return toResponse(entity);
        }
        WorkspaceChannelEntity e = existing.get();
        e.update(
                Boolean.TRUE.equals(payload.enabled()),
                blankToNull(payload.label()),
                mergedConfig,
                now
        );
        channelRepository.save(e);
        return toResponse(e);
    }

    /**
     * Пустые строки, {@code null} и маска {@code ***} в patch не перезаписывают уже сохранённые секреты.
     */
    private String mergeConfigJson(String existingJson, String patchJson) {
        try {
            ObjectNode base = (ObjectNode) objectMapper.readTree(
                    existingJson == null || existingJson.isBlank() ? "{}" : existingJson);
            ObjectNode patch = (ObjectNode) objectMapper.readTree(patchJson);
            Iterator<String> names = patch.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                JsonNode v = patch.get(key);
                if (v == null || v.isNull()) {
                    base.remove(key);
                    continue;
                }
                if (v.isTextual()) {
                    String t = v.asText();
                    if (t.isEmpty() || "***".equals(t)) {
                        continue;
                    }
                }
                base.set(key, v);
            }
            return objectMapper.writeValueAsString(base);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid channel config merge");
        }
    }

    @Transactional
    public void delete(Long workspaceId, ChannelType type) {
        var e = channelRepository.findByWorkspaceIdAndChannelType(workspaceId, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found"));
        channelRepository.delete(e);
    }

    private ChannelResponse toResponse(WorkspaceChannelEntity e) {
        return new ChannelResponse(
                e.getId(),
                e.getChannelType(),
                e.isEnabled(),
                e.getLabel(),
                maskConfig(e.getConfigJson()),
                e.getUpdatedAt()
        );
    }

    private void validateJson(String json) {
        try {
            objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "configJson must be valid JSON");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String maskConfig(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(maskNode(root));
        } catch (Exception e) {
            return "{}";
        }
    }

    private JsonNode maskNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode out = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode val = entry.getValue();
                if (shouldMaskKey(key)) {
                    out.set(key, TextNode.valueOf("***"));
                } else if (val.isObject() || val.isArray()) {
                    out.set(key, maskNode(val));
                } else {
                    out.set(key, val);
                }
            });
            return out;
        }
        if (node.isArray()) {
            ArrayNode arr = objectMapper.createArrayNode();
            for (JsonNode el : node) {
                arr.add(maskNode(el));
            }
            return arr;
        }
        return node;
    }

    private static boolean shouldMaskKey(String key) {
        String k = key.toLowerCase();
        return k.contains("token")
                || k.contains("secret")
                || k.contains("password")
                || k.contains("apikey")
                || k.endsWith("key");
    }
}
