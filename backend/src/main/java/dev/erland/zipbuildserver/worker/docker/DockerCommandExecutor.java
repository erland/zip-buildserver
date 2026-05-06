package dev.erland.zipbuildserver.worker.docker;

import dev.erland.zipbuildserver.domain.model.CheckStatus;
import dev.erland.zipbuildserver.worker.CommandExecutionRequest;
import dev.erland.zipbuildserver.worker.CommandExecutionResult;
import dev.erland.zipbuildserver.worker.CommandExecutor;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@IfBuildProperty(name = "zip-buildserver.worker.executor", stringValue = "docker")
@ApplicationScoped
public class DockerCommandExecutor implements CommandExecutor {
    private final ResourceLimitConfig resourceLimitConfig;

    public DockerCommandExecutor(ResourceLimitConfig resourceLimitConfig) {
        this.resourceLimitConfig = resourceLimitConfig;
    }

    @Override
    public CommandExecutionResult execute(CommandExecutionRequest request) {
        request.resolvedWorkingDirectory();

        Instant started = Instant.now();
        String containerName = "zip-buildserver-" + UUID.randomUUID();
        List<String> command = dockerRunCommand(request, containerName);

        try {
            Process process = new ProcessBuilder(command).start();
            LimitedOutput stdout = new LimitedOutput(process.getInputStream(), resourceLimitConfig.maxOutputBytes());
            LimitedOutput stderr = new LimitedOutput(process.getErrorStream(), resourceLimitConfig.maxOutputBytes());
            Thread stdoutThread = Thread.ofVirtual().start(stdout);
            Thread stderrThread = Thread.ofVirtual().start(stderr);

            boolean completed = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            Duration duration = Duration.between(started, Instant.now());

            if (!completed) {
                process.destroyForcibly();
                cleanupContainer(containerName);
                stdoutThread.join(Duration.ofSeconds(2));
                stderrThread.join(Duration.ofSeconds(2));
                return CommandExecutionResult.timedOut(
                        request.commandLabel(),
                        duration,
                        stdout.output(),
                        appendTruncation(stderr.output(), stderr.truncated()) + "\nCommand exceeded timeout of " + request.timeout().toSeconds() + " seconds.");
            }

            int exitCode = process.exitValue();
            stdoutThread.join(Duration.ofSeconds(2));
            stderrThread.join(Duration.ofSeconds(2));

            String stdoutText = appendTruncation(stdout.output(), stdout.truncated());
            String stderrText = appendTruncation(stderr.output(), stderr.truncated());

            if (exitCode == 0) {
                return CommandExecutionResult.passed(request.commandLabel(), duration, stdoutText, stderrText);
            }
            return CommandExecutionResult.failed(
                    request.commandLabel(),
                    exitCode,
                    duration,
                    stdoutText,
                    stderrText,
                    "Docker worker command exited with status " + exitCode + ".");
        } catch (IOException exception) {
            return new CommandExecutionResult(
                    request.commandLabel(),
                    CheckStatus.INTERNAL_ERROR,
                    -1,
                    Duration.between(started, Instant.now()),
                    false,
                    "",
                    exception.getMessage(),
                    "Could not start Docker worker command.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cleanupContainer(containerName);
            return new CommandExecutionResult(
                    request.commandLabel(),
                    CheckStatus.CANCELLED,
                    -1,
                    Duration.between(started, Instant.now()),
                    false,
                    "",
                    exception.getMessage(),
                    "Docker worker command was interrupted.");
        }
    }

    List<String> dockerRunCommand(CommandExecutionRequest request, String containerName) {
        String hostWorkspace = hostWorkspacePath(request.workspaceRoot().toAbsolutePath().normalize().toString());
        String containerWorkingDirectory = containerWorkingDirectory(request.workingDirectory());

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--name");
        command.add(containerName);
        command.add("--network");
        command.add(resourceLimitConfig.networkMode());
        command.add("--memory");
        command.add(resourceLimitConfig.memoryLimit());
        command.add("--cpus");
        command.add(resourceLimitConfig.cpuLimit());
        command.add("-v");
        command.add(hostWorkspace + ":/workspace:rw");
        command.add("-w");
        command.add(containerWorkingDirectory);
        command.add(resourceLimitConfig.image());
        command.add("/bin/sh");
        command.add("-lc");
        command.add(request.commandDisplay());
        return command;
    }



    private String hostWorkspacePath(String containerWorkspacePath) {
        String containerRoot = resourceLimitConfig.workspaceContainerDirectory();
        String hostRoot = resourceLimitConfig.workspaceHostDirectory();
        if (hostRoot == null || hostRoot.isBlank()) {
            return containerWorkspacePath;
        }
        java.nio.file.Path normalizedContainerRoot = java.nio.file.Path.of(containerRoot).toAbsolutePath().normalize();
        java.nio.file.Path normalizedWorkspace = java.nio.file.Path.of(containerWorkspacePath).toAbsolutePath().normalize();
        if (!normalizedWorkspace.startsWith(normalizedContainerRoot)) {
            return containerWorkspacePath;
        }
        java.nio.file.Path relative = normalizedContainerRoot.relativize(normalizedWorkspace);
        return java.nio.file.Path.of(hostRoot).toAbsolutePath().normalize().resolve(relative).toString();
    }

    private String containerWorkingDirectory(String workingDirectory) {
        if (workingDirectory == null || workingDirectory.isBlank() || ".".equals(workingDirectory)) {
            return "/workspace";
        }
        String normalized = workingDirectory.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return "/workspace/" + normalized;
    }

    private void cleanupContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName).start().waitFor(5, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String appendTruncation(String output, boolean truncated) {
        if (!truncated) {
            return output;
        }
        return output + "\n[output truncated]";
    }

    private static final class LimitedOutput implements Runnable {
        private final InputStream input;
        private final int maxBytes;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private volatile boolean truncated;

        private LimitedOutput(InputStream input, int maxBytes) {
            this.input = input;
            this.maxBytes = Math.max(0, maxBytes);
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8192];
            int read;
            try {
                while ((read = input.read(chunk)) != -1) {
                    int remaining = maxBytes - buffer.size();
                    if (remaining > 0) {
                        buffer.write(chunk, 0, Math.min(read, remaining));
                    }
                    if (read > remaining) {
                        truncated = true;
                    }
                }
            } catch (IOException exception) {
                truncated = true;
            }
        }

        private String output() {
            return buffer.toString(StandardCharsets.UTF_8);
        }

        private boolean truncated() {
            return truncated;
        }
    }
}
