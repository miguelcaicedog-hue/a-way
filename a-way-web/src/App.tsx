import { useEffect, useState, type FormEvent } from 'react'
import './App.css'
import { api } from './api'
import { ArbitrosScreen } from './Arbitros'
import { CategoryScreen, EditionScreen, LeagueScreen, PlayerScreen, TeamScreen } from './Catalog'
import { CalendarioScreen } from './Calendario'
import { CanchasScreen } from './Canchas'
import { FasesScreen } from './Fases'
import { GruposScreen } from './Grupos'
import { HorariosScreen } from './Horarios'
import { ResultadosScreen } from './Resultados'
import { InscripcionScreen } from './Inscripciones'
import { TarjetasCatalogoScreen } from './TarjetasCatalogo'

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
  { id: 'jugadores', label: 'Jugadores', summary: 'Ficha del plantel: cédula, nombre, alias, número de camisa, foto y rotación.' },
  { id: 'inscripciones', label: 'Inscripciones', summary: 'Carga del archivo de inscripciones y alta de equipos y jugadores por categoría.' },
  { id: 'grupos', label: 'Grupos', summary: 'Grupos de cada categoría y asignación de equipos, a mano o automática.' },
  { id: 'fases', label: 'Fases', summary: 'Fase de grupos, ida y vuelta, y cruces de octavos a final por posición.' },
  { id: 'calendario', label: 'Calendario', summary: 'Jornadas, fechas, horarios, cancha y árbitro de cada partido.' },
  { id: 'horarios', label: 'Horarios', summary: 'Cancha y hora de los partidos. Filtra por categoría y fecha, y asigna turnos desde la hora y el intervalo que elijas.' },
  { id: 'resultados', label: 'Resultados', summary: 'Marcador de los partidos de una jornada.' },
  { id: 'goles', label: 'Goles', summary: 'Goles de cada jugador por partido.' },
  { id: 'tarjetas', label: 'Tarjetas', summary: 'Amarillas y rojas de cada jugador por jornada.' },
  { id: 'tabla', label: 'Tabla', summary: 'Posiciones con diferencia de goles, goles convertidos y fair play.' },
  { id: 'arbitros', label: 'Árbitros', summary: 'Árbitros de la liga: cédula, nombre, teléfono, foto y contacto por WhatsApp.' },
  { id: 'canchas', label: 'Canchas', summary: 'Canchas de la liga, con búsqueda por descripción y estado.' },
  { id: 'catalogo-tarjetas', label: 'Tarjetas', summary: 'Tipos de tarjeta de la liga: color, costo y jornadas de suspensión.' },
  { id: 'suspensiones', label: 'Suspensiones', summary: 'Reglas por tarjetas y jornada en que empieza la sanción.' },
  { id: 'pagos', label: 'Pagos', summary: 'Arbitraje por jornada y cancelación de multas por tarjeta.' },
  { id: 'sanciones', label: 'Sanciones', summary: 'Quitar puntos, o puntos y goles, a un equipo.' },
  { id: 'importaciones', label: 'Importaciones', summary: 'Planteles desde Excel y equipos desde el formulario de Google.' },
  { id: 'configuracion', label: 'Configuración', summary: 'Fair play, momento de la suspensión y reglas de la liga.' },
  { id: 'usuarios', label: 'Usuarios', summary: 'Master, administrativo y asistente de cada liga.' },
] as const

const menuGroups = [
  { label: 'Inicio', ids: ['inicio'] },
  { label: 'Competencia', ids: ['ligas', 'ediciones', 'categorias', 'equipos', 'jugadores', 'inscripciones'] },
  { label: 'Fixture', ids: ['grupos', 'fases', 'calendario', 'horarios'] },
  { label: 'Jornada', ids: ['resultados', 'goles', 'tarjetas', 'tabla'] },
  { label: 'Disciplina', ids: ['catalogo-tarjetas', 'suspensiones', 'sanciones'] },
  { label: 'Cancha', ids: ['arbitros', 'canchas', 'pagos'] },
  { label: 'Administración', ids: ['importaciones', 'configuracion', 'usuarios'] },
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
  return membership.role === 'ADMINISTRATIVO' ? 'Administrativo' : 'Asistente'
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
  const [openMenu, setOpenMenu] = useState<string | null>(null)
  const [leagues, setLeagues] = useState<{ id: string; name: string; hasLogo: boolean; active: boolean }[]>([])
  const [editions, setEditions] = useState<{ id: string; name: string }[]>([])
  const [leagueId, setLeagueId] = useState('')
  const [editionId, setEditionId] = useState('')
  const [scopeVersion, setScopeVersion] = useState(0)

  function refreshScope(next?: { leagueId?: string; editionId?: string }) {
    if (next?.leagueId) {
      setLeagueId(next.leagueId)
    }
    if (next?.editionId) {
      setEditionId(next.editionId)
    }
    setScopeVersion((value) => value + 1)
  }

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

  useEffect(() => {
    if (!profile) {
      return
    }
    let cancelled = false
    api<{ id: string; name: string; hasLogo: boolean; active: boolean }[]>('/api/leagues')
      .then((items) => {
        if (cancelled) {
          return
        }
        const activas = items.filter((item) => item.active)
        setLeagues(activas)
        setLeagueId((current) => {
          const visible = (id: string) => activas.some((item) => item.id === id)
          if (!profile.master) {
            const preferida = profile.memberships[0]?.leagueId ?? ''
            if (preferida && visible(preferida)) {
              return preferida
            }
          }
          if (current && visible(current)) {
            return current
          }
          return activas[0]?.id ?? ''
        })
      })
      .catch(() => {
        if (!cancelled) {
          setLeagues([])
        }
      })
    return () => {
      cancelled = true
    }
  }, [profile, scopeVersion])

  useEffect(() => {
    if (!leagueId) {
      setEditions([])
      setEditionId('')
      return
    }
    let cancelled = false
    api<{ id: string; name: string }[]>(`/api/editions?leagueId=${leagueId}`)
      .then((items) => {
        if (cancelled) {
          return
        }
        setEditions(items)
        setEditionId((current) => (current && items.some((item) => item.id === current) ? current : items[0]?.id ?? ''))
      })
      .catch(() => {
        if (!cancelled) {
          setEditions([])
          setEditionId('')
        }
      })
    return () => {
      cancelled = true
    }
  }, [leagueId, scopeVersion])

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
      <aside className="sidebar">
        <div className="brand">A-WAY</div>
        <nav aria-label="Principal">
          {menuGroups.map((group) => {
            const items = group.ids.flatMap((id) => {
              const item = sections.find((sectionItem) => sectionItem.id === id)
              return item ? [item] : []
            })
            if (items.length === 1 && items[0].id === 'inicio') {
              return (
                <button
                  key={group.label}
                  type="button"
                  aria-current={section === 'inicio' ? 'page' : undefined}
                  onClick={() => setSection('inicio')}
                >
                  Inicio
                </button>
              )
            }
            const active = items.some((item) => item.id === section)
            return (
              <div
                key={group.label}
                className={openMenu === group.label ? 'menu-group is-open' : 'menu-group'}
                onMouseEnter={() => setOpenMenu(group.label)}
                onMouseLeave={() => setOpenMenu(null)}
              >
                <button
                  type="button"
                  className={active ? 'is-current' : undefined}
                  aria-expanded={openMenu === group.label}
                  aria-haspopup="true"
                  onClick={() => setOpenMenu(openMenu === group.label ? null : group.label)}
                >
                  {group.label}
                </button>
                <div className="menu-tree" role="group" aria-label={group.label}>
                  {items.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      aria-current={item.id === section ? 'page' : undefined}
                      onClick={() => {
                        setSection(item.id)
                        setOpenMenu(null)
                      }}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>
            )
          })}
        </nav>
      </aside>
      <div className="content">
        <header className="topbar">
          <div className="session">
          <label>
            Liga
            <select
              className="league-select"
              value={leagueId}
              disabled={!profile.master}
              onChange={(event) => setLeagueId(event.target.value)}
            >
              {leagues.map((league) => (
                <option key={league.id} value={league.id}>{league.name}</option>
              ))}
            </select>
          </label>
          <label>
            Edición
            <select value={editionId} onChange={(event) => setEditionId(event.target.value)}>
              {editions.map((edition) => (
                <option key={edition.id} value={edition.id}>{edition.name}</option>
              ))}
            </select>
          </label>
          {leagues.some((league) => league.id === leagueId && league.hasLogo) && (
            <img className="league-logo" alt="" src={`/api/leagues/${leagueId}/logo?v=${scopeVersion}`} />
          )}
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
        {section === 'ligas' && <LeagueScreen profile={profile} leagueId={leagueId} onScopeChange={refreshScope} />}
        {section === 'ediciones' && <EditionScreen profile={profile} leagueId={leagueId} onScopeChange={refreshScope} />}
        {section === 'categorias' && <CategoryScreen profile={profile} editionId={editionId} />}
        {section === 'equipos' && <TeamScreen profile={profile} leagueId={leagueId} editionId={editionId} />}
        {section === 'jugadores' && <PlayerScreen profile={profile} leagueId={leagueId} editionId={editionId} />}
        {section === 'inscripciones' && <InscripcionScreen profile={profile} leagueId={leagueId} editionId={editionId} />}
        {section === 'grupos' && <GruposScreen profile={profile} editionId={editionId} />}
        {section === 'fases' && <FasesScreen profile={profile} editionId={editionId} />}
        {section === 'calendario' && <CalendarioScreen profile={profile} editionId={editionId} />}
        {section === 'horarios' && <HorariosScreen profile={profile} editionId={editionId} />}
        {section === 'resultados' && <ResultadosScreen key={editionId} profile={profile} editionId={editionId} />}
        {section === 'arbitros' && <ArbitrosScreen profile={profile} leagueId={leagueId} />}
        {section === 'catalogo-tarjetas' && <TarjetasCatalogoScreen profile={profile} leagueId={leagueId} />}
        {section === 'canchas' && <CanchasScreen profile={profile} leagueId={leagueId} />}
      </main>
      </div>
    </div>
  )
}

export default App
