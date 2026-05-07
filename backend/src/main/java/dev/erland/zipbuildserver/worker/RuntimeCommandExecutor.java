package dev.erland.zipbuildserver.worker;

import dev.erland.zipbuildserver.worker.docker.DockerCommandExecutor;
import dev.erland.zipbuildserver.worker.fake.FakeCommandExecutor;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RuntimeCommandExecutor implements CommandExecutor {
    private final String executorName;
    private final FakeCommandExecutor fakeCommandExecutor;
    private final DockerCommandExecutor dockerCommandExecutor;

    public RuntimeCommandExecutor(
            @ConfigProperty(name = "zip-buildserver.worker.executor", defaultValue = "fake")
            String executorName,
            FakeCommandExecutor fakeCommandExecutor,
            DockerCommandExecutor dockerCommandExecutor) {
        this.executorName = executorName == null ? "fake" : executorName.trim().toLowerCase();
        this.fakeCommandExecutor = fakeCommandExecutor;
        this.dockerCommandExecutor = dockerCommandExecutor;
    }

    @Override
    public CommandExecutionResult execute(CommandExecutionRequest request) {
        return switch (executorName) {
            case "docker" -> dockerCommandExecutor.execute(request);
            case "fake" -> fakeCommandExecutor.execute(request);
            default -> throw new IllegalStateException(
                    "Unsupported worker executor '" + executorName + "'. Expected 'fake' or 'docker'.");
        };
    }
}
