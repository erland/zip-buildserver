package dev.erland.zipbuildserver.api.artifact;

import dev.erland.zipbuildserver.domain.model.ArtifactType;

import java.util.UUID;

public record ArtifactContentResponse(
        UUID id,
        UUID runId,
        ArtifactType type,
        String content) {
}
