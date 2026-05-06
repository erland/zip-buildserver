package dev.erland.zipbuildserver.api.assistant;

import dev.erland.zipbuildserver.domain.model.SessionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssistantSessionResponse(
        UUID sessionId,
        SessionStatus status,
        String label,
        String retentionPolicy,
        OffsetDateTime createdAt) {
}
