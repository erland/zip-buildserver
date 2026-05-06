package dev.erland.zipbuildserver.api.assistant;

import dev.erland.zipbuildserver.domain.model.CheckStatus;

public record AssistantCommandSummaryResponse(
        String label,
        String command,
        String workingDirectory,
        CheckStatus status,
        Integer exitCode,
        String failureCategory,
        String failureMessage,
        String logExcerpt) {
}
