package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.ChannelResponse;
import io.altacod.publisher.api.dto.ChannelUpsertPayload;
import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public List<ChannelResponse> list(@ActiveWorkspace Long workspaceId) {
        return channelService.list(workspaceId);
    }

    @PutMapping("/{type}")
    public ChannelResponse upsert(
            @ActiveWorkspace Long workspaceId,
            @PathVariable ChannelType type,
            @Valid @RequestBody ChannelUpsertPayload payload
    ) {
        return channelService.upsert(workspaceId, type, payload);
    }

    @DeleteMapping("/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@ActiveWorkspace Long workspaceId, @PathVariable ChannelType type) {
        channelService.delete(workspaceId, type);
    }
}
