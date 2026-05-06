import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { PackageUploadDropzone } from '../components/PackageUploadDropzone';
import { PollingRunStatus } from '../components/PollingRunStatus';
import { StatusBadge } from '../components/StatusBadge';
import { useCreateRun, useSessionRuns } from '../api/runs';
import { useSession } from '../api/sessions';
import styles from './Page.module.css';

export function SessionPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const sessionQuery = useSession(sessionId);
  const runsQuery = useSessionRuns(sessionId);
  const createRun = useCreateRun();

  async function handleStartRun(sourcePackageId: string) {
    if (!sessionId) {
      return;
    }

    const run = await createRun.mutateAsync({ sessionId, packageId: sourcePackageId });
    navigate(`/runs/${run.id}`);
  }

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
  const createRunError =
    createRun.error instanceof ApiError
      ? createRun.error.message
      : createRun.error
        ? 'Could not start verification run.'
        : null;

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
        <p>Upload a zip package. After upload, the frontend starts a verification run and opens the run report.</p>
        <PackageUploadDropzone sessionId={session.id} onUploadSuccess={handleStartRun} />
        {createRun.isPending ? <p role="status">Starting verification run…</p> : null}
        {createRunError ? <p role="alert">{createRunError}</p> : null}
      </div>

      <div className={styles.card}>
        <h2>Runs</h2>
        {runsQuery.isLoading ? <p>Loading runs…</p> : null}
        {runsQuery.isError ? <p>Could not load runs.</p> : null}
        {runsQuery.data && runsQuery.data.runs.length === 0 ? <p>No runs have been started yet.</p> : null}
        {runsQuery.data && runsQuery.data.runs.length > 0 ? (
          <ul>
            {runsQuery.data.runs.map((run) => (
              <li key={run.id}>
                <PollingRunStatus runId={run.id} />
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  );
}
