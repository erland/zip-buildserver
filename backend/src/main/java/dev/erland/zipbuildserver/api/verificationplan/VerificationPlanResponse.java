package dev.erland.zipbuildserver.api.verificationplan;

import dev.erland.zipbuildserver.domain.model.verification.VerificationPlan;

import java.util.List;

public record VerificationPlanResponse(List<VerificationPlan> plans) {
}
