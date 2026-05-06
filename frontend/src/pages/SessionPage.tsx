import { Link, useParams } from 'react-router-dom';
import { PackageUploadDropzone } from '../components/PackageUploadDropzone';
import { StatusBadge } from '../components/StatusBadge';
import { useSession } from '../api/sessions';
import styles from './Page.module.css';

export function SessionPage() {
  const { sessionId } = useParams();
  const sessionQuery = useSession(sessionId);

  if (!sessionId) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>
          <h2>Missing session</h2>
          <Link to="/">Create a new session</Link>
        </div>
      </section>
    );
  }

  if (sessionQuery.isLoading) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>Loading session…</div>
      </section>
    );
  }

  if (sessionQuery.isError) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>
          <h2>Could not load session</h2>
          <p>Check that the session exists and that the backend is running.</p>
          <Link to="/">Create a new session</Link>
        </div>
      </section>
    );
  }

  const session = sessionQuery.data;

  if (!session) {
    return (
      <section className={styles.page}>
        <div className={styles.card}>Session data was unavailable.</div>
      </section>
    );
  }

  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <StatusBadge label={session.status} />
        <h2>{session.label || 'Verification session'}</h2>
        <p>Session ID: <code>{session.id}</code></p>
        <p>Created: {new Date(session.createdAt).toLocaleString()}</p>
      </div>

      <div className={styles.card}>
        <h2>Package upload</h2>
        <p>Upload a zip package to validate it and detect supported Maven or Node project structures.</p>
        <PackageUploadDropzone sessionId={session.id} />
      </div>
    </section>
  );
}
