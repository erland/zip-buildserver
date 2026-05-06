package dev.erland.zipbuildserver.api.run;

import dev.erland.zipbuildserver.application.run.VerificationRunService;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api")
public class RunResource {
    private final VerificationRunService service;

    public RunResource(VerificationRunService service) {
        this.service = service;
    }

    @POST
    @Path("/sessions/{sessionId}/runs")
    public RestResponse<RunResponse> create(
            @PathParam("sessionId") UUID sessionId,
            @Valid CreateRunRequest request) {
        RunResponse response = service.create(sessionId, request);
        return RestResponse.status(RestResponse.Status.CREATED, response);
    }

    @GET
    @Path("/runs/{runId}")
    public RunResponse get(@PathParam("runId") UUID runId) {
        return service.get(runId);
    }

    @GET
    @Path("/runs/{runId}/summary")
    public RunSummaryResponse summary(@PathParam("runId") UUID runId) {
        return service.summary(runId);
    }

    @GET
    @Path("/sessions/{sessionId}/runs")
    public RunListResponse listForSession(@PathParam("sessionId") UUID sessionId) {
        return new RunListResponse(service.listForSession(sessionId));
    }
}
