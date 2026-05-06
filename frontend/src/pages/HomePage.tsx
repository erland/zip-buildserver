import { StatusBadge } from '../components/StatusBadge';
import styles from './Page.module.css';

export function HomePage() {
  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <StatusBadge label="MVP foundation" />
        <h2>Verify uploaded source packages without modifying code</h2>
        <p>
          This frontend will support creating verification sessions, uploading zip packages,
          starting predefined checks, and reading concise build and test summaries.
        </p>
      </div>
    </section>
  );
}
