package dev.erland.zipbuildserver.worker;

public interface CommandExecutor {
    CommandExecutionResult execute(CommandExecutionRequest request);
}
