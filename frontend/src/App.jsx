import { useAuth0 } from '@auth0/auth0-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { createPost, getMe, getPosts } from './api.js';
import './App.css';

function formatTime(iso) {
  try {
    return new Intl.DateTimeFormat('es', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

export default function App() {
  const {
    isAuthenticated,
    isLoading,
    loginWithRedirect,
    logout,
    user,
    getAccessTokenSilently
  } = useAuth0();

  const [posts, setPosts] = useState([]);
  const [streamError, setStreamError] = useState(null);
  const [streamLoading, setStreamLoading] = useState(true);

  const [draft, setDraft] = useState('');
  const [posting, setPosting] = useState(false);
  const [postError, setPostError] = useState(null);

  const [me, setMe] = useState(null);
  const [meError, setMeError] = useState(null);

  const audience = import.meta.env.VITE_AUTH0_AUDIENCE;

  const authParams = useMemo(
    () => ({
      authorizationParams: {
        audience,
        scope: 'openid profile email read:posts write:posts read:profile offline_access'
      }
    }),
    [audience]
  );

  const loadStream = useCallback(async () => {
    setStreamLoading(true);
    setStreamError(null);
    try {
      setPosts(await getPosts());
    } catch (e) {
      setStreamError(e.message || 'Error al cargar el stream');
    } finally {
      setStreamLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStream();
  }, [loadStream]);

  useEffect(() => {
    if (!isAuthenticated) {
      setMe(null);
      setMeError(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const token = await getAccessTokenSilently(authParams);
        const profile = await getMe(token);
        if (!cancelled) {
          setMe(profile);
          setMeError(null);
        }
      } catch (e) {
        if (!cancelled) {
          setMe(null);
          setMeError(e.message || 'No se pudo cargar /api/me');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, getAccessTokenSilently, authParams]);

  async function handleSubmit(e) {
    e.preventDefault();
    setPostError(null);
    const text = draft.trim();
    if (text.length === 0 || text.length > 140) {
      setPostError('El post debe tener entre 1 y 140 caracteres.');
      return;
    }
    setPosting(true);
    try {
      const token = await getAccessTokenSilently(authParams);
      await createPost(token, text);
      setDraft('');
      await loadStream();
    } catch (err) {
      setPostError(err.message || 'No se pudo publicar');
    } finally {
      setPosting(false);
    }
  }

  if (isLoading) {
    return (
      <div className="card center">
        <p className="muted">Cargando sesión…</p>
      </div>
    );
  }

  return (
    <div>
      <header className="header">
        <div>
          <h1 className="title">Lab 10 Stream</h1>
          <p className="subtitle">Stream público · posts con JWT (Auth0)</p>
        </div>
        <div className="header-actions">
          {isAuthenticated ? (
            <>
              <span className="pill">{user?.email || user?.name || 'Sesión activa'}</span>
              <button
                type="button"
                className="btn ghost"
                onClick={() =>
                  logout({ logoutParams: { returnTo: window.location.origin } })
                }
              >
                Cerrar sesión
              </button>
            </>
          ) : (
            <button type="button" className="btn primary" onClick={() => loginWithRedirect()}>
              Iniciar sesión
            </button>
          )}
        </div>
      </header>

      {isAuthenticated && (
        <section className="card profile">
          <h2>Tu perfil (GET /api/me)</h2>
          {meError && <p className="error">{meError}</p>}
          {me && (
            <dl className="dl">
              <div>
                <dt>sub</dt>
                <dd>{me.sub}</dd>
              </div>
              <div>
                <dt>email</dt>
                <dd>{me.email || '—'}</dd>
              </div>
              <div>
                <dt>nombre</dt>
                <dd>{me.name || '—'}</dd>
              </div>
            </dl>
          )}
        </section>
      )}

      {isAuthenticated && (
        <section className="card composer">
          <h2>Nuevo post</h2>
          <form onSubmit={handleSubmit}>
            <textarea
              className="textarea"
              rows={3}
              maxLength={140}
              placeholder="¿Qué está pasando? (máx. 140)"
              value={draft}
              onChange={(ev) => setDraft(ev.target.value)}
              disabled={posting}
            />
            <div className="composer-footer">
              <span className={draft.length > 140 ? 'count bad' : 'count'}>
                {draft.length}/140
              </span>
              <button type="submit" className="btn primary" disabled={posting}>
                {posting ? 'Publicando…' : 'Publicar'}
              </button>
            </div>
          </form>
          {postError && <p className="error">{postError}</p>}
        </section>
      )}

      {!isAuthenticated && (
        <p className="hint card">
          Inicia sesión para publicar y ver tu perfil. El stream es público para todos.
        </p>
      )}

      <section className="card stream">
        <div className="stream-head">
          <h2>Stream público</h2>
          <button type="button" className="btn ghost sm" onClick={loadStream} disabled={streamLoading}>
            Actualizar
          </button>
        </div>
        {streamError && <p className="error">{streamError}</p>}
        {streamLoading && <p className="muted">Cargando posts…</p>}
        {!streamLoading && posts.length === 0 && <p className="muted">Aún no hay posts.</p>}
        <ul className="posts">
          {posts.map((p) => (
            <li key={p.id} className="post">
              <div className="post-meta">
                <strong>{p.authorName || p.authorId}</strong>
                <span className="muted">{formatTime(p.createdAt)}</span>
              </div>
              <p className="post-body">{p.content}</p>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
