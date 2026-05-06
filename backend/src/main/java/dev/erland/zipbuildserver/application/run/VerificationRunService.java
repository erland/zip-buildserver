package dev.erland.zipbuildserver.application.run;

import dev.erland.zipbuildserver.api.run.CreateRunRequest;
import dev.erland.zipbuildserver.api.run.RunCommandResponse;
import dev.erland.zipbuildserver.api.run.RunResponse;
import dev.erland.zipbuildserver.api.run.RunSummaryResponse;
import dev.erland.zipbuildserver.application.NotFoundException;
import dev.erland.zipbuildserver.application.project.ProjectDetectionService;
import dev.erland.zipbuildserver.application.verification.VerificationPlanService;
import dev.erland.zipbuildserver.domain.model.CheckStatus;
import dev.erland.zipbuildserver.domain.model.NetworkMode;
import dev.erland.zipbuildserver.domain.model.RunStatus;
import dev.erland.zipbuildserver.domain.model.SessionStatus;
import dev.erland.zipbuildserver.domain.model.SourcePackageStatus;
import dev.erland.zipbuildserver.domain.model.project.DetectedProject;
import dev.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import dev.erland.zipbuildserver.domain.model.verification.VerificationPlan;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.entity.VerificationSessionEntity;
import dev.erland.zipbuildserver.infrastructure.persistence.repository.SourcePackageRepository;
import dev.erland.zipbuildserver.infrastructure.persistence.repository.VerificationCommandResultRepository;
import dev.erland.zipbuildserver.infrastructure.persistence.repository.VerificationRunRepository;
import dev.erland.zipbuildserver.infrastructure.persistence.repository.VerificationSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VerificationRunService {
    private final VerificationSessionRepository sessionRepository;
    private final SourcePackageRepository packageRepository;
    private final VerificationRunRepository runRepository;
    private final VerificationCommandResultRepository commandRepository;
    private final ProjectDetectionService projectDetectionService;
    private final VerificationPlanService verificationPlanService;

    public VerificationRunService(
            VerificationSessionRepository sessionRepository,
            SourcePackageRepository packageRepository,
            VerificationRunRepository runRepository,
            VerificationCommandResultRepository commandRepository,
            ProjectDetectionService projectDetectionService,
            VerificationPlanService verificationPlanService) {
        this.sessionRepository = sessionRepository;
        this.packageRepository = packageRepository;
        this.runRepository = runRepository;
        this.commandRepository = commandRepository;
        this.projectDetectionService = projectDetectionService;
        this.verificationPlanService = verificationPlanService;
    }

    @Transactional
    public RunResponse create(UUID sessionId, CreateRunRequest request) {
        VerificationSessionEntity session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new NotFoundException("Session was not found: " + sessionId));
        if (session.status == SessionStatus.CLOSED) {
            throw new BadRequestException("Cannot create a run for a closed session.");
        }

        UUID packageId = request == null ? null : request.packageId();
        if (packageId == null) {
            throw new BadRequestException("packageId is required.");
        }

        SourcePackageEntity sourcePackage = packageRepository.findByIdOptional(packageId)
                .orElseThrow(() -> new NotFoundException("Package was not found: " + packageId));
        if (!sourcePackage.sessionId.equals(sessionId)) {
            throw new BadRequestException("Package does not belong to the requested session.");
        }
        if (sourcePackage.status != SourcePackageStatus.ACCEPTED) {
            throw new BadRequestException("Cannot create a run for a rejected package.");
        }

        VerificationPlan plan = selectPlan(sourcePackage, normalize(request.requestedPlanId()));

        VerificationRunEntity entity = new VerificationRunEntity();
        entity.id = UUID.randomUUID();
        entity.sessionId = sessionId;
        entity.sourcePackageId = packageId;
        entity.status = RunStatus.QUEUED;
        entity.planId = plan.id();
        entity.requestedPlanId = normalize(request.requestedPlanId());
        entity.networkMode = NetworkMode.valueOf(plan.networkMode().name());
        entity.summary = "Run queued. Execution is not implemented until a later delivery step.";
        entity.startedAt = null;
        entity.completedAt = null;
        entity.durationMillis = null;

        runRepository.persist(entity);
        return toResponse(entity);
    }

    public RunResponse get(UUID runId) {
        VerificationRunEntity entity = runRepository.findByIdOptional(runId)
                .orElseThrow(() -> new NotFoundException("Run was not found: " + runId));
        return toResponse(entity);
    }

    public List<RunResponse> listForSession(UUID sessionId) {
        if (!sessionRepository.findByIdOptional(sessionId).isPresent()) {
            throw new NotFoundException("Session was not found: " + sessionId);
        }
        return runRepository.list("sessionId", sessionId).stream()
                .sorted(Comparator.comparing((VerificationRunEntity run) -> run.id.toString()))
                .map(this::toResponse)
                .toList();
    }

    public RunSummaryResponse summary(UUID runId) {
        VerificationRunEntity entity = runRepository.findByIdOptional(runId)
                .orElseThrow(() -> new NotFoundException("Run was not found: " + runId));
        List<VerificationCommandResultEntity> commands = commandsFor(entity.id);
        List<String> commandLabels = commands.stream()
                .map(command -> command.commandLabel)
                .toList();
        return new RunSummaryResponse(
                entity.id,
                entity.status,
                entity.summary,
                entity.planId,
                null,
                commandLabels,
                List.of("Execution service and command results are introduced in later steps."),
                false);
    }

    private VerificationPlan selectPlan(SourcePackageEntity sourcePackage, String requestedPlanId) {
        ProjectDetectionSummary detection = projectDetectionService.detect(Path.of(sourcePackage.storageReference));
        if (!detection.supported() || detection.projects().isEmpty()) {
            throw new BadRequestException("No supported verification plan was found for the package.");
        }

        if (requestedPlanId != null) {
            VerificationPlan requested = verificationPlanService.findById(requestedPlanId)
                    .orElseThrow(() -> new BadRequestException("Requested verification plan is not available: " + requestedPlanId));
            boolean compatible = detection.projects().stream()
                    .anyMatch(project -> project.technology() == requested.technology());
            if (!compatible) {
                throw new BadRequestException("Requested verification plan is not compatible with the detected package.");
            }
            return requested;
        }

        DetectedProject selectedProject = detection.projects().get(0);
        if (selectedProject.selectedPlanId() == null) {
            throw new BadRequestException("Detected project does not have a selected verification plan.");
        }
        return verificationPlanService.findById(selectedProject.selectedPlanId())
                .orElseThrow(() -> new BadRequestException("Selected verification plan is not available: " + selectedProject.selectedPlanId()));
    }

    private RunResponse toResponse(VerificationRunEntity entity) {
        return new RunResponse(
                entity.id,
                entity.sessionId,
                entity.sourcePackageId,
                entity.status,
                entity.planId,
                entity.requestedPlanId,
                entity.networkMode,
                entity.summary,
                entity.startedAt,
                entity.completedAt,
                entity.durationMillis,
                commandsFor(entity.id).stream().map(this::toCommandResponse).toList());
    }

    private RunCommandResponse toCommandResponse(VerificationCommandResultEntity entity) {
        return new RunCommandResponse(
                entity.id,
                entity.commandLabel,
                entity.workingDirectory,
                entity.commandDisplay,
                entity.status,
                entity.exitCode,
                entity.startedAt,
                entity.completedAt,
                entity.durationMillis,
                entity.logExcerpt,
                entity.failureCategory,
                entity.failureMessage,
                entity.stdoutArtifactRef,
                entity.stderrArtifactRef);
    }

    private List<VerificationCommandResultEntity> commandsFor(UUID runId) {
        return commandRepository.list("runId", runId).stream()
                .sorted(Comparator.comparing(command -> command.commandLabel))
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
