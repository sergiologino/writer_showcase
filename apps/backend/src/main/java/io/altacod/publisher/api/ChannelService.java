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
        if (existing.isEmpty()) {
            var entity = new WorkspaceChannelEntity(
                    ws,
                    type,
                    Boolean.TRUE.equals(payload.enabled()),
                    blankToNull(payload.label()),
                    payload.configJson(),
                    now
            );
            channelRepository.save(entity);
            return toResponse(entity);
        }
        WorkspaceChannelEntity e = existing.get();
        e.update(
                Boolean.TRUE.equals(payload.enabled()),
                blankToNull(payload.label()),
                payload.configJson(),
                now
        );
        channelRepository.save(e);
        return toResponse(e);
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
