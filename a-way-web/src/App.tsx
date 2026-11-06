import { useEffect, useState } from 'react'
import './App.css'

type Health = {
  service?: string
  status?: string
  database?: string
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetch(`${apiBase}/api/health`)
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        return (await response.json()) as Health
      })
      .then((body) => {
        if (!cancelled) {
          setHealth(body)
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : 'No se pudo consultar la API')
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main>
      <h1>A-WAY</h1>
      <p>Estado de la plataforma</p>
      {error && <p className="down">API: {error}</p>}
      {health && (
        <ul>
          <li>Servicio: {health.service}</li>
          <li>API: {health.status}</li>
          <li>Base de datos: {health.database}</li>
        </ul>
      )}
      {!error && !health && <p>Consultando /api/health…</p>}
    </main>
  )
}

export default App
