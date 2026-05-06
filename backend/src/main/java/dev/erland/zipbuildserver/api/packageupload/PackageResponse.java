package dev.erland.zipbuildserver.api.packageupload;

import dev.erland.zipbuildserver.domain.model.SourcePackageStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PackageResponse(
        UUID id,
        UUID sessionId,
        String originalFilename,
        String checksumSha256,
        long compressedSizeBytes,
        Long extractedSizeBytes,
        Integer fileCount,
        String topLevelEntries,
        String storageReference,
        SourcePackageStatus status,
        String rejectionReason,
        OffsetDateTime createdAt) {
}
