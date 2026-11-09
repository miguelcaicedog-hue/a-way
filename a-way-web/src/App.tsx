import { useEffect, useState, type FormEvent } from 'react'
import './App.css'

type Health = {
  service?: string
  status?: string
  database?: string
}

type Membership = {
  leagueId: string
  leagueName: string
  role: string
}

type Profile = {
  username: string
  displayName: string
  master: boolean
  memberships: Membership[]
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

const sections = [
  { id: 'inicio', label: 'Inicio', summary: 'Estado de la plataforma y la sesión activa.' },
  { id: 'ligas', label: 'Ligas', summary: 'Alta de ligas, vigencia y acceso. Solo el master crea ligas.' },
  { id: 'ediciones', label: 'Ediciones', summary: 'Temporadas de cada liga.' },
  { id: 'categorias', label: 'Categorías', summary: 'Categorías de una edición, tope de jugadores y criterios de desempate.' },
  { id: 'equipos', label: 'Equipos', summary: 'Clubes de la liga e inscripción a una categoría.' },
  { id: 'jugadores', label: 'Jugadores', summary: 'Ficha del plantel: nombre, alias, número, foto y rotación.' },
  { id: 'grupos', label: 'Grupos', summary: 'De uno a n grupos, cargados a mano, al azar o por sorteo.' },
  { id: 'fases', label: 'Fases', summary: 'Fase de grupos, ida y vuelta, y cruces de octavos a final por posición.' },
  { id: 'calendario', label: 'Calendario', summary: 'Jornadas, fechas, horarios, cancha y árbitro de cada partido.' },
  { id: 'resultados', label: 'Resultados', summary: 'Marcador de los partidos de una jornada.' },
  { id: 'goles', label: 'Goles', summary: 'Goles de cada jugador por partido.' },
  { id: 'tarjetas', label: 'Tarjetas', summary: 'Amarillas y rojas de cada jugador por jornada.' },
  { id: 'tabla', label: 'Tabla', summary: 'Posiciones con diferencia de goles, goles convertidos y fair play.' },
  { id: 'arbitros', label: 'Árbitros', summary: 'Árbitros de la categoría y árbitro por defecto de cada cancha.' },
  { id: 'canchas', label: 'Canchas', summary: 'Canchas de la categoría y asignación a cada partido.' },
  { id: 'suspensiones', label: 'Suspensiones', summary: 'Reglas por tarjetas y jornada en que empieza la sanción.' },
  { id: 'pagos', label: 'Pagos', summary: 'Arbitraje por jornada y cancelación de multas por tarjeta.' },
  { id: 'sanciones', label: 'Sanciones', summary: 'Quitar puntos, o puntos y goles, a un equipo.' },
  { id: 'importaciones', label: 'Importaciones', summary: 'Planteles desde Excel y equipos desde el formulario de Google.' },
  { id: 'configuracion', label: 'Configuración', summary: 'Fair play, momento de la suspensión y reglas de la liga.' },
  { id: 'usuarios', label: 'Usuarios', summary: 'Master, administrativo y asistente de cada liga.' },
] as const

type SectionId = (typeof sections)[number]['id']

function roleLabel(profile: Profile) {
  if (profile.master) {
    return 'Master'
  }
  const membership = profile.memberships[0]
  if (!membership) {
    return 'Sin rol'
  }
  const role = membership.role === 'ADMINISTRATIVO' ? 'Administrativo' : 'Asistente'
  return `${role} · ${membership.leagueName}`
}

function App() {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [checkingSession, setCheckingSession] = useState(true)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loginError, setLoginError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [health, setHealth] = useState<Health | null>(null)
  const [section, setSection] = useState<SectionId>('inicio')

  useEffect(() => {
    let cancelled = false
    fetch(`${apiBase}/api/auth/me`, { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) {
          return null
        }
        return (await response.json()) as Profile
      })
      .then((body) => {
        if (!cancelled) {
          setProfile(body)
          setCheckingSession(false)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCheckingSession(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!profile) {
      return
    }
    let cancelled = false
    fetch(`${apiBase}/api/health`, { credentials: 'include' })
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
      .catch(() => {
        if (!cancelled) {
          setHealth(null)
        }
      })
    return () => {
      cancelled = true
    }
  }, [profile])

  async function onLogin(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setLoginError(null)
    try {
      const response = await fetch(`${apiBase}/api/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      if (!response.ok) {
        setLoginError('Usuario o clave incorrectos')
        return
      }
      setProfile((await response.json()) as Profile)
      setPassword('')
    } catch {
      setLoginError('No se pudo contactar la API')
    } finally {
      setSubmitting(false)
    }
  }

  async function onLogout() {
    await fetch(`${apiBase}/api/auth/logout`, { method: 'POST', credentials: 'include' })
    setProfile(null)
    setHealth(null)
  }

  if (checkingSession) {
    return (
      <main>
        <h1>A-WAY</h1>
        <p>Comprobando la sesión…</p>
      </main>
    )
  }

  if (!profile) {
    return (
      <main>
        <h1>A-WAY</h1>
        <p>Ingreso a la administración</p>
        <form onSubmit={onLogin}>
          <label>
            Usuario
            <input
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
          </label>
          <label>
            Clave
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {loginError && <p className="down">{loginError}</p>}
          <button type="submit" disabled={submitting}>
            Entrar
          </button>
        </form>
      </main>
    )
  }

  const current = sections.find((item) => item.id === section) ?? sections[0]

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">A-WAY</div>
        <nav aria-label="Principal">
          {sections.map((item) => (
            <button
              key={item.id}
              type="button"
              aria-current={item.id === section ? 'page' : undefined}
              onClick={() => setSection(item.id)}
            >
              {item.label}
            </button>
          ))}
        </nav>
        <div className="session">
          <span>{roleLabel(profile)}</span>
          <button type="button" onClick={onLogout}>
            Salir
          </button>
        </div>
      </header>
      <main className="workspace">
        <h1>{current.label}</h1>
        <p>{current.summary}</p>
        {section === 'inicio' && health && (
          <ul>
            <li>Servicio: {health.service}</li>
            <li>API: {health.status}</li>
            <li>Base de datos: {health.database}</li>
          </ul>
        )}
      </main>
    </div>
  )
}

export default App
