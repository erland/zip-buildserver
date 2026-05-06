package dev.erland.zipbuildserver.api.run;

import dev.erland.zipbuildserver.domain.model.CheckStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RunCommandResponse(
        UUID id,
        String commandLabel,
        String workingDirectory,
        String commandDisplay,
        CheckStatus status,
        Integer exitCode,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long durationMillis,
        String logExcerpt,
        String failureCategory,
        String failureMessage,
        UUID stdoutArtifactRef,
        UUID stderrArtifactRef) {
}
