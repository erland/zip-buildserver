export type RunStatus = 'queued' | 'running' | 'passed' | 'failed' | 'rejected' | 'timed_out' | 'cancelled' | 'incomplete' | 'internal_error';

export interface VerificationPlanSummary {
  id: string;
  displayName: string;
  supportedProjectIndicators: string[];
}

export interface HealthResponse {
  status: string;
  service: string;
}
