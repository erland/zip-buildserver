import type { RunSummary } from '../api/types';
import styles from './FailureSummaryCard.module.css';

interface FailureSummaryCardProps {
  summary: RunSummary;
}

export function FailureSummaryCard({ summary }: FailureSummaryCardProps) {
  if (!summary.primaryFailure && summary.suggestedFocus.length === 0 && !summary.partial) {
    return null;
  }

  return (
    <aside className={styles.failure} aria-label="Failure summary">
      <h3>{summary.primaryFailure ? 'Primary failure' : 'Run warning'}</h3>
      {summary.primaryFailure ? <p>{summary.primaryFailure}</p> : null}
      {summary.partial ? <p>This result is partial. Some commands may not have completed.</p> : null}
      {summary.suggestedFocus.length > 0 ? (
        <>
          <strong>Suggested focus</strong>
          <ul className={styles.list}>
            {summary.suggestedFocus.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </>
      ) : null}
    </aside>
  );
}
