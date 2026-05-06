package dev.erland.zipbuildserver.worker.fake;

import dev.erland.zipbuildserver.worker.CommandExecutionRequest;
import dev.erland.zipbuildserver.worker.CommandExecutionResult;
import dev.erland.zipbuildserver.worker.CommandExecutor;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class FakeCommandExecutor implements CommandExecutor {
    private final Map<String, Deque<CommandExecutionResult>> resultsByLabel = new HashMap<>();

    public FakeCommandExecutor returns(CommandExecutionResult result) {
        resultsByLabel
                .computeIfAbsent(result.commandLabel(), ignored -> new ArrayDeque<>())
                .addLast(result);
        return this;
    }

    @Override
    public CommandExecutionResult execute(CommandExecutionRequest request) {
        request.resolvedWorkingDirectory();

        Deque<CommandExecutionResult> configuredResults = resultsByLabel.get(request.commandLabel());
        if (configuredResults != null && !configuredResults.isEmpty()) {
            return configuredResults.removeFirst();
        }

        return CommandExecutionResult.passed(
                request.commandLabel(),
                Duration.ZERO,
                "fake executor completed: " + request.commandDisplay(),
                "");
    }
}
