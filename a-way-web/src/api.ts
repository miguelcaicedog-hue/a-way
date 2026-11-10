const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  })
  if (response.status === 204) {
    return undefined as T
  }
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    const message = typeof body.error === 'string' ? body.error : `HTTP ${response.status}`
    throw new Error(message)
  }
  return body as T
}
