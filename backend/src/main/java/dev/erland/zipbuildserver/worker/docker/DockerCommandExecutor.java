package dev.erland.zipbuildserver.worker.docker;

import dev.erland.zipbuildserver.worker.CommandExecutionRequest;
import dev.erland.zipbuildserver.worker.CommandExecutionResult;
import dev.erland.zipbuildserver.worker.CommandExecutor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DockerCommandExecutor implements CommandExecutor {
    @Override
    public CommandExecutionResult execute(CommandExecutionRequest request) {
        throw new UnsupportedOperationException(
                "Docker command execution is not implemented yet. "
                        + "Step 14 will execute approved commands in worker containers.");
    }
}
