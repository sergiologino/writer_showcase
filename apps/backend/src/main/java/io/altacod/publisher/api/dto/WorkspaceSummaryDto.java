package io.altacod.publisher.api.dto;

import io.altacod.publisher.workspace.MembershipRole;

public record WorkspaceSummaryDto(long id, String name, String slug, MembershipRole role) {
}
