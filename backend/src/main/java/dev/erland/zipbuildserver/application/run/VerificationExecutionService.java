package dev.erland.zipbuildserver.application.run;

import dev.erland.zipbuildserver.application.project.ProjectDetectionService;
import dev.erland.zipbuildserver.domain.model.ArtifactType;
import dev.erland.zipbuildserver.domain.model.CheckStatus;
import dev.erland.zipbuildserver.domain.model.RunStatus;
import dev.erland.zipbuildserver.domain.model.project.DetectedProject;
import dev.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import dev.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import dev.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.repository.VerificationCommandResultRepository;
import dev.erland.zipbuildserver.infrastructure.persistence.repository.VerificationRunRepository;
import dev.erland.zipbuildserver.storage.ArtifactStorageService;
import dev.erland.zipbuildserver.worker.CommandExecutionRequest;
import dev.erland.zipbuildserver.worker.CommandExecutionResult;
import dev.erland.zipbuildserver.worker.CommandExecutor;
import dev.erland.zipbuildserver.worker.docker.DockerWorkspaceService;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VerificationExecutionService {
    private final VerificationRunRepository runRepository;
    private final VerificationCommandResultRepository commandRepository;
    private final ProjectDetectionService projectDetectionService;
    private final CommandExecutor commandExecutor;
    private final LogExcerptService logExcerptService;
    private final FailureClassificationService failureClassificationService;
    private final DockerWorkspaceService workspaceService;
    private final ArtifactStorageService artifactStorageService;

    public VerificationExecutionService(
            VerificationRunRepository runRepository,
            VerificationCommandResultRepository commandRepository,
            ProjectDetectionService projectDetectionService,
            CommandExecutor commandExecutor,
            LogExcerptService logExcerptService,
            FailureClassificationService failureClassificationService,
            DockerWorkspaceService workspaceService,
            ArtifactStorageService artifactStorageService) {
        this.runRepository = runRepository;
        this.commandRepository = commandRepository;
        this.projectDetectionService = projectDetectionService;
        this.commandExecutor = commandExecutor;
        this.logExcerptService = logExcerptService;
        this.failureClassificationService = failureClassificationService;
        this.workspaceService = workspaceService;
        this.artifactStorageService = artifactStorageService;
    }

    public void execute(VerificationRunEntity run, SourcePackageEntity sourcePackage, VerificationPlan plan) {
        OffsetDateTime started = OffsetDateTime.now();
        run.status = RunStatus.RUNNING;
        run.startedAt = started;
        run.summary = "Verification is running.";
        runRepository.persist(run);

        Path packagePath = Path.of(sourcePackage.storageReference);
        Path workspaceRoot = workspaceService.createWorkspace(packagePath);
        DetectedProject project = selectDetectedProject(packagePath, plan);

        boolean failed = false;
        boolean timedOut = false;
        try {
            for (VerificationCommand command : plan.commands()) {
                if (failed || timedOut) {
                    persistSkipped(run.id, command, resolveWorkingDirectory(command.workingDirectory(), project), "Skipped because an earlier command failed.");
                    continue;
                }

                String workingDirectory = resolveWorkingDirectory(command.workingDirectory(), project);
                CommandExecutionRequest request = new CommandExecutionRequest(
                        command.label(),
                        workspaceRoot,
                        workingDirectory,
                        command.commandDisplay(),
                        Duration.ofSeconds(command.timeoutSeconds()));

                CommandExecutionResult result = commandExecutor.execute(request);
                persistResult(run.id, command, workingDirectory, result);

                if (result.status() == CheckStatus.TIMED_OUT) {
                    timedOut = true;
                } else if (result.status() == CheckStatus.FAILED || result.status() == CheckStatus.INTERNAL_ERROR) {
                    failed = true;
                }
            }
        } finally {
            workspaceService.cleanup(workspaceRoot);
        }

        OffsetDateTime completed = OffsetDateTime.now();
        run.completedAt = completed;
        run.durationMillis = Duration.between(started, completed).toMillis();
        run.status = timedOut ? RunStatus.TIMED_OUT : failed ? RunStatus.FAILED : RunStatus.PASSED;
        run.summary = summaryFor(run.status, plan.commands().size());
    }

    private DetectedProject selectDetectedProject(Path packagePath, VerificationPlan plan) {
        ProjectDetectionSummary detection = projectDetectionService.detect(packagePath);
        return detection.projects().stream()
                .filter(project -> project.technology() == plan.technology())
                .findFirst()
                .orElseGet(() -> detection.projects().isEmpty()
                        ? new DetectedProject(".", plan.technology(), List.of(), plan.id(), "Fallback project for fake execution.")
                        : detection.projects().get(0));
    }

    private void persistResult(UUID runId, VerificationCommand command, String workingDirectory, CommandExecutionResult result) {
        VerificationCommandResultEntity entity = new VerificationCommandResultEntity();
        OffsetDateTime started = OffsetDateTime.now().minus(result.duration());
        OffsetDateTime completed = OffsetDateTime.now();
        entity.id = UUID.randomUUID();
        entity.runId = runId;
        entity.commandLabel = command.label();
        entity.workingDirectory = workingDirectory;
        entity.commandDisplay = command.commandDisplay();
        entity.status = result.status();
        entity.exitCode = result.exitCode();
        entity.startedAt = started;
        entity.completedAt = completed;
        entity.durationMillis = result.duration().toMillis();
        entity.logExcerpt = logExcerptService.excerpt(result.stdout(), result.stderr());
        entity.failureCategory = result.status() == CheckStatus.PASSED ? null : failureClassificationService.category(result);
        entity.failureMessage = result.status() == CheckStatus.PASSED ? null : failureClassificationService.message(result);
        entity.stdoutArtifactRef = artifactStorageService.storeText(runId, ArtifactType.STDOUT, command.label(), result.stdout()).id;
        entity.stderrArtifactRef = artifactStorageService.storeText(runId, ArtifactType.STDERR, command.label(), result.stderr()).id;
        commandRepository.persist(entity);
    }

    private void persistSkipped(UUID runId, VerificationCommand command, String workingDirectory, String reason) {
        VerificationCommandResultEntity entity = new VerificationCommandResultEntity();
        OffsetDateTime now = OffsetDateTime.now();
        entity.id = UUID.randomUUID();
        entity.runId = runId;
        entity.commandLabel = command.label();
        entity.workingDirectory = workingDirectory;
        entity.commandDisplay = command.commandDisplay();
        entity.status = CheckStatus.SKIPPED;
        entity.exitCode = null;
        entity.startedAt = now;
        entity.completedAt = now;
        entity.durationMillis = 0L;
        entity.logExcerpt = "";
        entity.failureCategory = "skipped";
        entity.failureMessage = reason;
        commandRepository.persist(entity);
    }

    private String resolveWorkingDirectory(String workingDirectory, DetectedProject project) {
        String projectPath = project.path() == null || project.path().isBlank() ? "." : project.path();
        return workingDirectory.replace("${project.path}", projectPath);
    }

    private String summaryFor(RunStatus status, int commandCount) {
        return switch (status) {
            case PASSED -> "Verification passed. " + commandCount + " approved command(s) completed.";
            case FAILED -> "Verification failed. Review command-level failure details.";
            case TIMED_OUT -> "Verification timed out. Review command-level timeout details.";
            default -> "Verification completed with status " + status + ".";
        };
    }
}
