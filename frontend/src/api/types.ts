export type RunStatus = 'QUEUED' | 'RUNNING' | 'PASSED' | 'FAILED' | 'REJECTED' | 'TIMED_OUT' | 'CANCELLED' | 'INCOMPLETE' | 'INTERNAL_ERROR';

export type SessionStatus = 'OPEN' | 'CLOSED';

export type SourcePackageStatus = 'ACCEPTED' | 'REJECTED';

export interface VerificationPlanSummary {
  id: string;
  displayName: string;
  supportedProjectIndicators: string[];
}

export interface HealthResponse {
  status: string;
  service: string;
}

export interface VerificationSession {
  id: string;
  label: string | null;
  status: SessionStatus;
  createdAt: string;
  closedAt: string | null;
  createdBy: string | null;
  retentionPolicy: string | null;
}

export interface CreateSessionRequest {
  label?: string;
  retentionPolicy?: string;
}

export interface DetectedProject {
  path: string;
  technology: string;
  buildIndicators: string[];
  selectedPlanId: string | null;
  selectionReason: string;
}

export interface ProjectDetectionSummary {
  projects: DetectedProject[];
  supported: boolean;
  summary: string;
}

export interface SourcePackage {
  id: string;
  sessionId: string;
  originalFilename: string;
  checksumSha256: string;
  compressedSizeBytes: number;
  extractedSizeBytes: number | null;
  fileCount: number | null;
  topLevelEntries: string | null;
  status: SourcePackageStatus;
  rejectionReason: string | null;
  createdAt: string;
  projectDetection: ProjectDetectionSummary | null;
}
