package io.altacod.publisher.api.dto;

import java.util.List;

public record MeResponse(UserSummaryDto user, List<WorkspaceSummaryDto> workspaces) {
}
