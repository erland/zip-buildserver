package info.isaksson.erland.zipbuildserver.api.assistant;

import info.isaksson.erland.zipbuildserver.api.run.CreateRunRequest;
import info.isaksson.erland.zipbuildserver.api.run.RunCommandResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunSummaryResponse;
import info.isaksson.erland.zipbuildserver.api.session.CreateSessionRequest;
import info.isaksson.erland.zipbuildserver.api.session.SessionResponse;
import info.isaksson.erland.zipbuildserver.application.VerificationSessionService;
import info.isaksson.erland.zipbuildserver.application.run.VerificationRunService;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/assistant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AssistantVerificationResource {
    private final VerificationSessionService sessionService;
    private final VerificationRunService runService;

    public AssistantVerificationResource(
            VerificationSessionService sessionService,
            VerificationRunService runService) {
        this.sessionService = sessionService;
        this.runService = runService;
    }

    @POST
    @Path("/verification-sessions")
    public RestResponse<AssistantSessionResponse> createSession(@Valid AssistantCreateSessionRequest request) {
        SessionResponse session = sessionService.create(new CreateSessionRequest(
                request == null ? null : request.label(),
                request == null ? null : request.retentionPolicy()));
        return RestResponse.status(RestResponse.Status.CREATED, toAssistantSession(session));
    }

    @POST
    @Path("/verification-sessions/{sessionId}/runs")
    public RestResponse<AssistantRunResponse> createRun(
            @PathParam("sessionId") UUID sessionId,
            @Valid AssistantCreateRunRequest request) {
        RunResponse run = runService.create(sessionId, new CreateRunRequest(
                request == null ? null : request.packageId(),
                request == null ? null : request.requestedPlanId()));
        return RestResponse.status(RestResponse.Status.CREATED, toAssistantRun(run));
    }

    @GET
    @Path("/verification-runs/{runId}/summary")
    public AssistantRunSummaryResponse summary(@PathParam("runId") UUID runId) {
        return toAssistantSummary(runService.get(runId), runService.summary(runId));
    }

    @GET
    @Path("/verification-runs/{runId}/failed-log-excerpts")
    public AssistantFailedLogExcerptResponse failedLogExcerpts(@PathParam("runId") UUID runId) {
        RunResponse run = runService.get(runId);
        List<AssistantCommandSummaryResponse> failed = run.commands().stream()
                .filter(command -> command.status() == CheckStatus.FAILED
                        || command.status() == CheckStatus.TIMED_OUT
                        || command.status() == CheckStatus.INTERNAL_ERROR)
                .map(this::toAssistantCommand)
                .toList();
        return new AssistantFailedLogExcerptResponse(run.id(), failed);
    }

    private AssistantSessionResponse toAssistantSession(SessionResponse session) {
        return new AssistantSessionResponse(
                session.id(),
                session.status(),
                session.label(),
                session.retentionPolicy(),
                session.createdAt());
    }

    private AssistantRunResponse toAssistantRun(RunResponse run) {
        return new AssistantRunResponse(
                run.id(),
                run.sessionId(),
                run.sourcePackageId(),
                run.status(),
                run.summary(),
                run.planId(),
                toAssistantSummary(run, runService.summary(run.id())));
    }

    private AssistantRunSummaryResponse toAssistantSummary(RunResponse run, RunSummaryResponse summary) {
        List<AssistantCommandSummaryResponse> failedChecks = run.commands().stream()
                .filter(command -> command.status() == CheckStatus.FAILED
                        || command.status() == CheckStatus.TIMED_OUT
                        || command.status() == CheckStatus.INTERNAL_ERROR)
                .map(this::toAssistantCommand)
                .toList();
        return new AssistantRunSummaryResponse(
                run.id(),
                run.status(),
                summary.summary(),
                summary.primaryFailure(),
                List.of(),
                List.of(),
                summary.commandsRun(),
                failedChecks,
                summary.suggestedFocus(),
                "/api/runs/%s/artifacts".formatted(run.id()),
                summary.partial());
    }

    private AssistantCommandSummaryResponse toAssistantCommand(RunCommandResponse command) {
        return new AssistantCommandSummaryResponse(
                command.commandLabel(),
                command.commandDisplay(),
                command.workingDirectory(),
                command.status(),
                command.exitCode(),
                command.failureCategory(),
                command.failureMessage(),
                command.logExcerpt());
    }
}
