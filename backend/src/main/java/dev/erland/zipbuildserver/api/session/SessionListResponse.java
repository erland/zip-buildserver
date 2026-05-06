package dev.erland.zipbuildserver.api.session;

import java.util.List;

public record SessionListResponse(List<SessionResponse> sessions) {
}
