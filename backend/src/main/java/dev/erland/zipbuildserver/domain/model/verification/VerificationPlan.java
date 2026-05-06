package dev.erland.zipbuildserver.domain.model.verification;

import dev.erland.zipbuildserver.domain.model.project.ProjectTechnology;

import java.util.List;

public record VerificationPlan(
        String id,
        String name,
        ProjectTechnology technology,
        List<String> indicators,
        List<VerificationCommand> commands,
        NetworkMode networkMode,
        boolean enabled,
        String selectionReason) {
}
