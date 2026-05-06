import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';

function renderApp(initialEntries = ['/']) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={initialEntries}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('App', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the service title and navigation', () => {
    renderApp();

    expect(screen.getByRole('heading', { name: /build and test verification/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Plans' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'About' })).toBeInTheDocument();
  });

  it('creates a session and navigates to package upload', async () => {
    const user = userEvent.setup();

    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: 'session-1',
            label: 'Test session',
            status: 'OPEN',
            createdAt: '2026-01-01T00:00:00Z',
            closedAt: null,
            createdBy: null,
            retentionPolicy: null,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: 'session-1',
            label: 'Test session',
            status: 'OPEN',
            createdAt: '2026-01-01T00:00:00Z',
            closedAt: null,
            createdBy: null,
            retentionPolicy: null,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      );

    renderApp();

    await user.type(screen.getByLabelText(/session label/i), 'Test session');
    await user.click(screen.getByRole('button', { name: /create session/i }));

    expect(await screen.findByRole('heading', { name: 'Test session' })).toBeInTheDocument();
    expect(screen.getByText(/Package upload/i)).toBeInTheDocument();
  });

  it('uploads a package from the session page', async () => {
    const user = userEvent.setup();

    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: 'session-1',
            label: 'Upload session',
            status: 'OPEN',
            createdAt: '2026-01-01T00:00:00Z',
            closedAt: null,
            createdBy: null,
            retentionPolicy: null,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: 'package-1',
            sessionId: 'session-1',
            originalFilename: 'source.zip',
            checksumSha256: 'abc',
            compressedSizeBytes: 10,
            extractedSizeBytes: 20,
            fileCount: 2,
            topLevelEntries: 'README.md',
            storageReference: 'packages/package-1.zip',
            status: 'ACCEPTED',
            rejectionReason: null,
            createdAt: '2026-01-01T00:00:00Z',
            projectDetection: {
              projects: [],
              supported: false,
              summary: 'No supported project detected.',
            },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      );

    renderApp(['/sessions/session-1']);

    const file = new File(['zip-content'], 'source.zip', { type: 'application/zip' });
    await user.upload(await screen.findByLabelText(/upload source-code zip/i), file);
    await user.click(screen.getByRole('button', { name: /upload package/i }));

    await waitFor(() => {
      expect(screen.getByText(/Uploaded source.zip as ACCEPTED/i)).toBeInTheDocument();
    });
  });
});
