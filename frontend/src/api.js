const base = () => import.meta.env.VITE_API_BASE_URL || '';

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
  const res = await fetch(`${base()}/api/posts`);
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  return res.json();
}

export async function createPost(accessToken, content) {
  const res = await fetch(`${base()}/api/posts`, {
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
  const res = await fetch(`${base()}/api/me`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  return res.json();
}
