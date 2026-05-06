package dev.erland.zipbuildserver.api.artifact;

import dev.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import dev.erland.zipbuildserver.storage.ArtifactStorageService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.UUID;

@Path("/api")
public class ArtifactResource {
    private final ArtifactStorageService artifactStorageService;

    public ArtifactResource(ArtifactStorageService artifactStorageService) {
        this.artifactStorageService = artifactStorageService;
    }

    @GET
    @Path("/runs/{runId}/artifacts")
    public ArtifactListResponse listForRun(@PathParam("runId") UUID runId) {
        return new ArtifactListResponse(artifactStorageService.listForRun(runId).stream()
                .map(this::toResponse)
                .toList());
    }

    @GET
    @Path("/artifacts/{artifactId}")
    public ArtifactContentResponse get(@PathParam("artifactId") UUID artifactId) {
        ArtifactReferenceEntity artifact = artifactStorageService.get(artifactId);
        return new ArtifactContentResponse(
                artifact.id,
                artifact.runId,
                artifact.type,
                artifactStorageService.readText(artifactId));
    }

    private ArtifactResponse toResponse(ArtifactReferenceEntity artifact) {
        return new ArtifactResponse(
                artifact.id,
                artifact.runId,
                artifact.type,
                artifact.sizeBytes,
                artifact.createdAt,
                artifact.expiresAt);
    }
}
