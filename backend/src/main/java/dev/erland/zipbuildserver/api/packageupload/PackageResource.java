package dev.erland.zipbuildserver.api.packageupload;

import dev.erland.zipbuildserver.application.SourcePackageService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api")
public class PackageResource {
    private final SourcePackageService sourcePackageService;

    public PackageResource(SourcePackageService sourcePackageService) {
        this.sourcePackageService = sourcePackageService;
    }

    @POST
    @Path("/sessions/{sessionId}/packages")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public RestResponse<PackageResponse> submit(
            @PathParam("sessionId") UUID sessionId,
            @MultipartForm PackageUploadForm form) {
        if (form == null || form.file == null) {
            throw new jakarta.ws.rs.BadRequestException("Multipart field 'file' is required.");
        }
        PackageResponse response = sourcePackageService.submit(sessionId, form.file.uploadedFile(), form.file.fileName());
        return RestResponse.status(RestResponse.Status.CREATED, response);
    }

    @GET
    @Path("/packages/{packageId}")
    public PackageResponse get(@PathParam("packageId") UUID packageId) {
        return sourcePackageService.get(packageId);
    }
}
