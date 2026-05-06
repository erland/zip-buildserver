package dev.erland.zipbuildserver.worker.fake;

import dev.erland.zipbuildserver.worker.CommandExecutionRequest;
import dev.erland.zipbuildserver.worker.CommandExecutionResult;
import dev.erland.zipbuildserver.worker.CommandExecutor;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@IfBuildProperty(name = "zip-buildserver.worker.executor", stringValue = "fake", enableIfMissing = true)
@ApplicationScoped
public final class FakeCommandExecutor implements CommandExecutor {
    private final Map<String, Deque<CommandExecutionResult>> resultsByLabel = new HashMap<>();

    public synchronized FakeCommandExecutor returns(CommandExecutionResult result) {
        resultsByLabel
                .computeIfAbsent(result.commandLabel(), ignored -> new ArrayDeque<>())
                .addLast(result);
        return this;
    }

    public synchronized void reset() {
        resultsByLabel.clear();
    }

    @Override
    public synchronized CommandExecutionResult execute(CommandExecutionRequest request) {
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
