package info.isaksson.erland.zipbuildserver.worker.docker;

import info.isaksson.erland.zipbuildserver.worker.CommandExecutionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerCommandExecutorTest {
    @TempDir
    Path workspace;

    @Test
    void buildsDockerRunCommandWithWorkspaceResourceLimitsAndApprovedCommand() {
        ResourceLimitConfig limits = new ResourceLimitConfig(
                "zip-buildserver-worker-node-maven:local",
                "2g",
                "2",
                "bridge",
                4096,
                workspace.getParent().toString(),
                workspace.getParent().toString());
        DockerCommandExecutor executor = new DockerCommandExecutor(limits);
        CommandExecutionRequest request = new CommandExecutionRequest(
                "maven-test",
                workspace,
                "backend",
                "mvn test",
                Duration.ofMinutes(10));

        List<String> command = executor.dockerRunCommand(request, "zip-buildserver-test");

        assertEquals("docker", command.get(0));
        assertTrue(command.contains("--rm"));
        assertTrue(command.contains("--memory"));
        assertTrue(command.contains("2g"));
        assertTrue(command.contains("--cpus"));
        assertTrue(command.contains("2"));
        assertTrue(command.contains("--network"));
        assertTrue(command.contains("bridge"));
        assertTrue(command.contains(workspace.toAbsolutePath().normalize() + ":/workspace:rw"));
        assertTrue(command.contains("/workspace/backend"));
        assertEquals("mvn test", command.get(command.size() - 1));
    }
}
