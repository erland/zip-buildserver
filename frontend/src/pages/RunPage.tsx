import { Link, useParams } from 'react-router-dom';
import { ArtifactList } from '../components/ArtifactList';
import { CommandResultTable } from '../components/CommandResultTable';
import { FailureSummaryCard } from '../components/FailureSummaryCard';
import { LogExcerptPanel } from '../components/LogExcerptPanel';
import { RunStatusBadge } from '../components/RunStatusBadge';
import { useRun, useRunSummary } from '../api/runs';
import styles from './Page.module.css';

function formatDuration(durationMillis: number | null): string {
  if (durationMillis === null) {
    return '—';
  }

  if (durationMillis < 1000) {
    return `${durationMillis} ms`;
  }

  return `${(durationMillis / 1000).toFixed(1)} s`;
}

export function RunPage() {
  const { runId } = useParams();
  const runQuery = useRun(runId);
  const summaryQuery = useRunSummary(runId);

  if (!runId) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>
          <h2>Missing run</h2>
          <Link to="/">Create a new session</Link>
        </div>
      </section>
    );
  }

  if (runQuery.isLoading) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>Loading run…</div>
      </section>
    );
  }

  if (runQuery.isError || !runQuery.data) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>
          <h2>Could not load run</h2>
          <p>Check that the run exists and that the backend is running.</p>
          <Link to="/">Create a new session</Link>
        </div>
      </section>
    );
  }

  const run = runQuery.data;
  const summary = summaryQuery.data;

  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <RunStatusBadge status={run.status} />
        <h2>Verification run</h2>
        <p>Run ID: <code>{run.id}</code></p>
        <p>Session ID: <Link to={`/sessions/${run.sessionId}`}>{run.sessionId}</Link></p>
        <p>Plan: {run.planId ?? 'Not selected yet'}</p>
        <p>Network mode: {run.networkMode ?? 'Not recorded'}</p>
        <p>Duration: {formatDuration(run.durationMillis)}</p>
        {run.summary ? <p>{run.summary}</p> : null}
      </div>

      {summary ? (
        <div className={styles.card}>
          <h2>Summary</h2>
          <p>{summary.summary ?? 'No summary is available yet.'}</p>
          <p>Commands run: {summary.commandsRun.length > 0 ? summary.commandsRun.join(', ') : 'None yet'}</p>
          <FailureSummaryCard summary={summary} />
        </div>
      ) : null}

      <div className={styles.card}>
        <h2>Command results</h2>
        <CommandResultTable commands={run.commands} />
      </div>

      <div className={styles.card}>
        <h2>Log excerpts</h2>
        <LogExcerptPanel commands={run.commands} />
      </div>

      <div className={styles.card}>
        <h2>Artifacts</h2>
        <ArtifactList runId={run.id} />
      </div>
    </section>
  );
}
