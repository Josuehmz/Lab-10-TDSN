const monolithBase = () => (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

/**
 * Base del HTTP API desplegado con SAM (un solo API Gateway para User + Posts + Stream).
 * Si está vacío, se usa el monolito (`VITE_API_BASE_URL`) con prefijo `/api`.
 */
const msBase = () => (import.meta.env.VITE_MICROSERVICES_BASE_URL || '').replace(/\/$/, '');

async function parseError(res) {
  const text = await res.text();
  try {
    const j = JSON.parse(text);
    return j.message || j.error || text;
  } catch {
    return text || res.statusText;
  }
}

export async function getPosts() {
  const ms = msBase();
  const url = ms ? `${ms}/posts` : `${monolithBase()}/api/posts`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  return res.json();
}

export async function createPost(accessToken, content) {
  const ms = msBase();
  const url = ms ? `${ms}/posts` : `${monolithBase()}/api/posts`;
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify({ content })
  });
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  return res.json();
}

export async function getMe(accessToken) {
  const ms = msBase();
  const url = ms ? `${ms}/me` : `${monolithBase()}/api/me`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  return res.json();
}
