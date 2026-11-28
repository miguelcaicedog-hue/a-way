import { useEffect, useRef, useState, type FormEvent } from 'react'
import logoSistema from './assets/logo-sistema.png'
import './App.css'
import { api } from './api'
import { ArbitrosScreen } from './Arbitros'
import { CategoryScreen, EditionScreen, LeagueScreen, PlayerScreen, TeamScreen } from './Catalog'
import { CalendarioScreen } from './Calendario'
import { ClasificacionScreen } from './Clasificacion'
import { CanchasScreen } from './Canchas'
import { FasesScreen } from './Fases'
import { GruposScreen } from './Grupos'
import { HorariosScreen } from './Horarios'
import { ResultadosScreen } from './Resultados'
import { GoleadoresScreen } from './Goleadores'
import { TablaScreen } from './Tabla'
import { InscripcionScreen } from './Inscripciones'
import { RefuerzoScreen } from './Refuerzos'
import { SancionesJugadorScreen } from './SancionesJugador'
import { SuspensionesScreen } from './Suspensiones'
import { SuspensionesJugadorScreen } from './SuspensionesJugador'
import { TarjetasCatalogoScreen } from './TarjetasCatalogo'
import { DesempateGoleadoresScreen } from './DesempateGoleadores'
import { DesempateEquiposScreen } from './DesempateEquipos'
import { PublicacionesScreen } from './Publicaciones'

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
  { id: 'inscripciones', label: 'Inscripción equipo', summary: 'Carga del archivo de inscripciones y alta de equipos y jugadores por categoría.' },
  { id: 'refuerzos', label: 'Inscripción refuerzo', summary: 'Carga de refuerzos para añadir o reemplazar jugadores en equipos ya inscritos.' },
  { id: 'grupos', label: 'Grupos', summary: 'Grupos de cada categoría y asignación de equipos, a mano o automática.' },
  { id: 'fases', label: 'Fases', summary: 'Fase de grupos, ida y vuelta, y cruces de octavos a final por posición.' },
  { id: 'clasificacion', label: 'Clasificación', summary: 'Cuadro de los equipos clasificados según grupos, tabla y fases.' },
  { id: 'calendario', label: 'Calendario', summary: 'Jornadas, fechas, horarios, cancha y árbitro de cada partido.' },
  { id: 'horarios', label: 'Horarios', summary: 'Cancha y hora de los partidos. Filtra por categoría y fecha, y asigna turnos desde la hora y el intervalo que elijas.' },
  { id: 'resultados', label: 'Resultados', summary: 'Marcador de los partidos de una jornada.' },
  { id: 'goles', label: 'Goleadores', summary: 'Goleadores de una categoría al cierre de la fecha elegida, con el detalle por jornada.' },
  { id: 'tabla', label: 'Tabla', summary: 'Posiciones de una categoría al cierre de la fecha elegida: puntos, diferencia y goles.' },
  { id: 'arbitros', label: 'Árbitros', summary: 'Árbitros de la liga: cédula, nombre, teléfono, foto y contacto por WhatsApp.' },
  { id: 'canchas', label: 'Canchas', summary: 'Canchas de la liga, con búsqueda por descripción y estado.' },
  { id: 'catalogo-tarjetas', label: 'Tarjetas', summary: 'Tipos de tarjeta de la liga: color, costo y jornadas de suspensión.' },
  { id: 'sanciones-jugador', label: 'Multas económicas', summary: 'Sanciones de jugador de la liga: descripción, costo y estado.' },
  { id: 'suspensiones', label: 'Suspensiones', summary: 'Jornadas de suspensión por tarjeta roja, acumulación de amarillas o rojas, y falta grave.' },
  { id: 'suspensiones-jugador', label: 'Suspender jugador', summary: 'Suspensiones pendientes de cada jugador y altas o bajas manuales.' },
  { id: 'desempate-goleadores', label: 'Desempate goleadores', summary: 'Orden de los criterios de desempate de la tabla de goleadores de la edición.' },
  { id: 'desempate-equipos', label: 'Desempate equipos', summary: 'Orden de los criterios de desempate de la tabla de posiciones de cada grupo.' },
  { id: 'publicaciones', label: 'Publicaciones', summary: 'Estilo del gráfico de la liga y descarga de goleadores, resultados, horarios y tabla de posiciones.' },
] as const

type SectionId = (typeof sections)[number]['id']

type MenuNodo = SectionId | { label: string; ids: readonly SectionId[] }

const menuGroups: { label: string; ids: readonly MenuNodo[] }[] = [
  { label: 'Inicio', ids: ['inicio'] },
  { label: 'Competencia', ids: ['ligas', 'ediciones', 'categorias'] },
  { label: 'Inscripción', ids: ['inscripciones', 'refuerzos'] },
  { label: 'Club', ids: ['equipos', 'jugadores'] },
  { label: 'Fixture', ids: ['grupos', 'fases', 'clasificacion', 'calendario', 'horarios'] },
  { label: 'Jornada', ids: ['resultados', 'goles', 'tabla', 'suspensiones-jugador'] },
  { label: 'Administración', ids: [{ label: 'General', ids: ['canchas', 'arbitros'] }, { label: 'Disciplina', ids: ['sanciones-jugador', 'catalogo-tarjetas', 'suspensiones'] }, { label: 'Criterio de desempate', ids: ['desempate-goleadores', 'desempate-equipos'] }, 'publicaciones'] },
]

const ACCESOS_INICIALES: SectionId[] = ['resultados', 'horarios', 'tabla', 'goles', 'calendario']

function claveUso(usuario: string) {
  return `away-menu-uso:${usuario}`
}

function leerUso(usuario: string) {
  try {
    const texto = localStorage.getItem(claveUso(usuario))
    if (!texto) return {}
    const valor = JSON.parse(texto) as Record<string, unknown>
    const uso: Partial<Record<SectionId, number>> = {}
    for (const seccion of sections) {
      const cantidad = valor[seccion.id]
      if (typeof cantidad === 'number' && cantidad > 0) {
        uso[seccion.id] = cantidad
      }
    }
    return uso
  } catch {
    return {}
  }
}

function sumarUso(usuario: string, id: SectionId) {
  const uso = leerUso(usuario)
  uso[id] = (uso[id] ?? 0) + 1
  localStorage.setItem(claveUso(usuario), JSON.stringify(uso))
}

function accesosUsados(usuario: string, staff: boolean) {
  const uso = leerUso(usuario)
  const visibles = sections.filter((item) => item.id !== 'inicio' && (item.id !== 'suspensiones-jugador' || staff))
  const ordenMenu = new Map(visibles.map((item, indice) => [item.id, indice]))
  const visitadas = visibles
    .filter((item) => (uso[item.id] ?? 0) > 0)
    .sort((izquierda, derecha) => {
      const diferencia = (uso[derecha.id] ?? 0) - (uso[izquierda.id] ?? 0)
      if (diferencia !== 0) return diferencia
      return (ordenMenu.get(izquierda.id) ?? 0) - (ordenMenu.get(derecha.id) ?? 0)
    })
  const elegidas = [...visitadas]
  for (const id of ACCESOS_INICIALES) {
    if (elegidas.length >= 5) break
    const item = visibles.find((seccion) => seccion.id === id)
    if (item && !elegidas.some((seccion) => seccion.id === item.id)) {
      elegidas.push(item)
    }
  }
  return elegidas.slice(0, 5)
}

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

function IconoMenu({ nombre }: { nombre: string }) {
  const trazo = {
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  }
  let dibujo
  switch (nombre) {
    case 'Inicio':
      dibujo = <path {...trazo} d="M4 10.5 12 4l8 6.5V20H15v-6H9v6H4z" />
      break
    case 'Competencia':
      dibujo = <path {...trazo} d="M8 4h8v3a4 4 0 0 1-8 0zm4 7v3m-4 6h8M7 20h10" />
      break
    case 'Ligas':
      dibujo = <path {...trazo} d="M12 3 5 6v6c0 4.5 3 7 7 8 4-1 7-3.5 7-8V6z" />
      break
    case 'Ediciones':
      dibujo = <><rect {...trazo} x="4" y="5" width="16" height="15" rx="2" /><path {...trazo} d="M8 3v4M16 3v4M4 10h16" /></>
      break
    case 'Categorías':
      dibujo = <path {...trazo} d="M4 8h10l6 4-6 4H4z" />
      break
    case 'Inscripción':
    case 'Inscripción equipo':
      dibujo = <><path {...trazo} d="M8 3h6l4 4v14H8z" /><path {...trazo} d="M14 3v4h4M10 13h6M10 17h4" /></>
      break
    case 'Inscripción refuerzo':
      dibujo = <><circle {...trazo} cx="10" cy="8" r="3" /><path {...trazo} d="M4 19c1-3 3.2-4.5 6-4.5M17 11v6M14 14h6" /></>
      break
    case 'Club':
    case 'Jugadores':
      dibujo = <><circle {...trazo} cx="12" cy="8" r="3" /><path {...trazo} d="M5 19c1.2-3 3.6-4.5 7-4.5s5.8 1.5 7 4.5" /></>
      break
    case 'Equipos':
      dibujo = <path {...trazo} d="M12 3 5 6v6c0 4.5 3 7 7 8 4-1 7-3.5 7-8V6z" />
      break
    case 'Fixture':
    case 'Grupos':
      dibujo = <><rect {...trazo} x="4" y="4" width="7" height="7" rx="1" /><rect {...trazo} x="13" y="4" width="7" height="7" rx="1" /><rect {...trazo} x="4" y="13" width="7" height="7" rx="1" /><rect {...trazo} x="13" y="13" width="7" height="7" rx="1" /></>
      break
    case 'Fases':
      dibujo = <path {...trazo} d="M7 5h4v4H7zM7 15h4v4H7zM13 10h4v4h-4zM11 7h2M11 17h2M13 12H11" />
      break
    case 'Clasificación':
      dibujo = <path {...trazo} d="M5 5h5v4H5zM14 5h5v4h-5zM5 15h5v4H5zM14 15h5v4h-5zM10 7h4M10 17h4M12 7v10" />
      break
    case 'Calendario':
      dibujo = <><rect {...trazo} x="4" y="5" width="16" height="15" rx="2" /><path {...trazo} d="M8 3v4M16 3v4M4 10h16M8 14h2M12 14h2M16 14h0M8 17h2" /></>
      break
    case 'Horarios':
      dibujo = <><circle {...trazo} cx="12" cy="12" r="8" /><path {...trazo} d="M12 8v5l3 2" /></>
      break
    case 'Jornada':
      dibujo = <path {...trazo} d="M6 4v16M6 5h9l-2 3 2 3H6" />
      break
    case 'Resultados':
      dibujo = <path {...trazo} d="M5 7h14v10H5zM12 7v10M8 11h1M15 11h1" />
      break
    case 'Goleadores':
    case 'Desempate goleadores':
      dibujo = <><circle {...trazo} cx="12" cy="12" r="8" /><path {...trazo} d="M12 4v3M12 17v3M4 12h3M17 12h3M7.5 7.5l2 2M14.5 14.5l2 2M16.5 7.5l-2 2M9.5 14.5l-2 2" /></>
      break
    case 'Tabla':
      dibujo = <path {...trazo} d="M5 6h14M5 12h14M5 18h14M8 6v12" />
      break
    case 'Suspender jugador':
      dibujo = <><circle {...trazo} cx="10" cy="8" r="3" /><path {...trazo} d="M4 19c1-3 3-4.5 6-4.5M16 13l4 4M20 13l-4 4" /></>
      break
    case 'Administración':
      dibujo = <><circle {...trazo} cx="12" cy="12" r="3" /><path {...trazo} d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.5 1.5M16.9 16.9l1.5 1.5M18.4 5.6l-1.5 1.5M7.1 16.9l-1.5 1.5" /></>
      break
    case 'General':
      dibujo = <path {...trazo} d="M4 8h10M18 8h2M4 16h2M10 16h10M14 6v4M8 14v4" />
      break
    case 'Canchas':
      dibujo = <><rect {...trazo} x="3" y="6" width="18" height="12" rx="1" /><path {...trazo} d="M12 6v12M3 12h18" /></>
      break
    case 'Árbitros':
      dibujo = <path {...trazo} d="M8 8a4 4 0 1 1 6 3.5V14H10v-2.5A4 4 0 0 1 8 8zM9 17h6M10 20h4" />
      break
    case 'Disciplina':
    case 'Tarjetas':
      dibujo = <rect {...trazo} x="7" y="3" width="10" height="18" rx="1.5" />
      break
    case 'Multas económicas':
      dibujo = <><circle {...trazo} cx="12" cy="12" r="8" /><path {...trazo} d="M12 7v10M9.5 9.5c.6-1 1.4-1.5 2.5-1.5 1.4 0 2.5.8 2.5 2s-1 2-2.5 2-2.5.8-2.5 2 1.1 2 2.5 2c1.1 0 1.9-.5 2.5-1.5" /></>
      break
    case 'Suspensiones':
      dibujo = <><circle {...trazo} cx="12" cy="12" r="8" /><path {...trazo} d="m7 7 10 10" /></>
      break
    case 'Criterio de desempate':
    case 'Desempate equipos':
      dibujo = <path {...trazo} d="M12 4v16M8 20h8M6 8h4l2-3 2 3h4M6 8l-2 5h6zM18 8l-2 5h6z" />
      break
    case 'Publicaciones':
      dibujo = <><rect {...trazo} x="4" y="5" width="16" height="14" rx="2" /><circle {...trazo} cx="9" cy="10" r="1.5" /><path {...trazo} d="m4 16 5-4 3 3 2-2 6 5" /></>
      break
    default:
      return null
  }
  return (
    <svg className="menu-icono" viewBox="0 0 24 24" aria-hidden="true">
      {dibujo}
    </svg>
  )
}

function App() {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [checkingSession, setCheckingSession] = useState(true)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loginError, setLoginError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [section, setSection] = useState<SectionId>('inicio')
  const [equipoJugadores, setEquipoJugadores] = useState<{ categoryId: string; teamId: string } | null>(null)
  const [openMenu, setOpenMenu] = useState<string | null>(null)
  const cierreMenu = useRef<number | null>(null)
  const [openSubmenu, setOpenSubmenu] = useState<string | null>(null)
  const cierreSubmenu = useRef<number | null>(null)
  const [leagues, setLeagues] = useState<{ id: string; name: string; hasLogo: boolean; active: boolean }[]>([])
  const [editions, setEditions] = useState<{ id: string; name: string }[]>([])
  const [leagueId, setLeagueId] = useState('')
  const [editionId, setEditionId] = useState('')
  const [scopeVersion, setScopeVersion] = useState(0)

  function abrirSeccion(id: SectionId, equipo?: { categoryId: string; teamId: string }) {
    if (id === 'jugadores') {
      setEquipoJugadores(equipo ?? null)
    }
    if (id !== 'inicio' && profile) {
      sumarUso(profile.username, id)
    }
    setSection(id)
    if (cierreMenu.current != null) {
      window.clearTimeout(cierreMenu.current)
      cierreMenu.current = null
    }
    setOpenMenu(null)
    setOpenSubmenu(null)
  }

  function abrirMenu(label: string) {
    if (cierreMenu.current != null) {
      window.clearTimeout(cierreMenu.current)
      cierreMenu.current = null
    }
    setOpenMenu(label)
  }

  function abrirSubmenu(label: string) {
    if (cierreSubmenu.current != null) {
      window.clearTimeout(cierreSubmenu.current)
      cierreSubmenu.current = null
    }
    setOpenSubmenu(label)
  }

  function programarCierreSubmenu() {
    if (cierreSubmenu.current != null) {
      window.clearTimeout(cierreSubmenu.current)
    }
    cierreSubmenu.current = window.setTimeout(() => {
      cierreSubmenu.current = null
      setOpenSubmenu(null)
    }, 1000)
  }

  function programarCierreMenu() {
    if (cierreMenu.current != null) {
      window.clearTimeout(cierreMenu.current)
    }
    cierreMenu.current = window.setTimeout(() => {
      cierreMenu.current = null
      setOpenMenu(null)
      setOpenSubmenu(null)
    }, 1000)
  }

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

  useEffect(() => () => {
    if (cierreMenu.current != null) {
      window.clearTimeout(cierreMenu.current)
    }
    if (cierreSubmenu.current != null) {
      window.clearTimeout(cierreSubmenu.current)
    }
  }, [])

  async function onLogout() {
    await fetch(`${apiBase}/api/auth/logout`, { method: 'POST', credentials: 'include' })
    setProfile(null)
  }

  if (checkingSession) {
    return (
      <div className="login-screen">
        <main>
          <div className="login-logo">
            <img src={logoSistema} alt="A-WAY" />
          </div>
          <p>Comprobando la sesión…</p>
        </main>
        <footer className="login-pie">Created by Miguel Caicedo © All rights reserved</footer>
      </div>
    )
  }

  if (!profile) {
    return (
      <div className="login-screen">
        <main>
          <div className="login-logo">
            <img src={logoSistema} alt="A-WAY" />
          </div>
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
        <footer className="login-pie">Created by Miguel Caicedo © All rights reserved</footer>
      </div>
    )
  }

  const current = sections.find((item) => item.id === section) ?? sections[0]

  return (
    <div className="app">
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <img src={logoSistema} alt="A-WAY" />
        </div>
        <nav aria-label="Principal">
          {menuGroups.map((group) => {
            const staff = profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
            const seccionesDe = (id: SectionId) => {
              const item = sections.find((sectionItem) => sectionItem.id === id)
              if (!item || (item.id === 'suspensiones-jugador' && !staff)) {
                return []
              }
              return [item]
            }
            const items = group.ids.flatMap((nodo) => typeof nodo === 'string' ? seccionesDe(nodo) : nodo.ids.flatMap(seccionesDe))
            if (items.length === 1 && items[0].id === 'inicio') {
              return (
                <button
                  key={group.label}
                  type="button"
                  aria-current={section === 'inicio' ? 'page' : undefined}
                  onClick={() => setSection('inicio')}
                >
                  <IconoMenu nombre="Inicio" />
                  Inicio
                </button>
              )
            }
            const active = items.some((item) => item.id === section)
            return (
              <div
                key={group.label}
                className={openMenu === group.label ? 'menu-group is-open' : 'menu-group'}
                onMouseEnter={() => abrirMenu(group.label)}
                onMouseLeave={programarCierreMenu}
              >
                <button
                  type="button"
                  className={active ? 'is-current' : undefined}
                  aria-expanded={openMenu === group.label}
                  aria-haspopup="true"
                  onClick={() => {
                    if (cierreMenu.current != null) {
                      window.clearTimeout(cierreMenu.current)
                      cierreMenu.current = null
                    }
                    setOpenMenu(openMenu === group.label ? null : group.label)
                  }}
                >
                  <IconoMenu nombre={group.label} />
                  {group.label}
                </button>
                <div className="menu-tree" role="group" aria-label={group.label}>
                  {group.ids.map((nodo) => {
                    if (typeof nodo === 'string') {
                      const item = seccionesDe(nodo)[0]
                      if (!item) {
                        return null
                      }
                      return (
                        <button
                          key={item.id}
                          type="button"
                          aria-current={item.id === section ? 'page' : undefined}
                          onClick={() => abrirSeccion(item.id)}
                        >
                          <IconoMenu nombre={item.label} />
                          {item.label}
                        </button>
                      )
                    }
                    const hijos = nodo.ids.flatMap(seccionesDe)
                    if (hijos.length === 0) {
                      return null
                    }
                    const submenuAbierto = openSubmenu === nodo.label
                    const submenuActivo = hijos.some((item) => item.id === section)
                    return (
                      <div
                        key={nodo.label}
                        className={submenuAbierto ? 'menu-group is-open' : 'menu-group'}
                        onMouseEnter={() => abrirSubmenu(nodo.label)}
                        onMouseLeave={programarCierreSubmenu}
                      >
                        <button
                          type="button"
                          className={submenuActivo ? 'is-current' : undefined}
                          aria-expanded={submenuAbierto}
                          aria-haspopup="true"
                          onClick={() => {
                            if (cierreSubmenu.current != null) {
                              window.clearTimeout(cierreSubmenu.current)
                              cierreSubmenu.current = null
                            }
                            setOpenSubmenu(submenuAbierto ? null : nodo.label)
                          }}
                        >
                          <IconoMenu nombre={nodo.label} />
                          {nodo.label}
                        </button>
                        <div className="menu-tree" role="group" aria-label={nodo.label}>
                          {hijos.map((item) => (
                            <button
                              key={item.id}
                              type="button"
                              aria-current={item.id === section ? 'page' : undefined}
                              onClick={() => abrirSeccion(item.id)}
                            >
                              <IconoMenu nombre={item.label} />
                              {item.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    )
                  })}
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
            {leagues.some((league) => league.id === leagueId && league.hasLogo) && (
              <img className="league-logo" alt="" src={`/api/leagues/${leagueId}/logo?v=${scopeVersion}`} />
            )}
          </label>
          <label>
            Edición
            <select value={editionId} onChange={(event) => setEditionId(event.target.value)}>
              {editions.map((edition) => (
                <option key={edition.id} value={edition.id}>{edition.name}</option>
              ))}
            </select>
          </label>
          <span>{roleLabel(profile)}</span>
          <button type="button" onClick={onLogout}>
            Salir
          </button>
          </div>
        </header>
        <main className={section === 'calendario' || section === 'horarios' || section === 'resultados' || section === 'goles' || section === 'tabla' || section === 'suspensiones' ? 'workspace estrecho' : 'workspace'}>
        <h1>{current.label}</h1>
        {section !== 'inicio' && <p>{current.summary}</p>}
        {section === 'inicio' && (
          <>
            <p className="accesos-titulo"><span aria-hidden="true">★</span> Accesos directos</p>
            <div className="accesos-inicio">
            {accesosUsados(
              profile.username,
              profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO'),
            ).map((item) => (
              <button key={item.id} type="button" onClick={() => abrirSeccion(item.id)}>
                <IconoMenu nombre={item.label} />
                <span>{item.label}</span>
              </button>
            ))}
            </div>
          </>
        )}
        {section === 'ligas' && <LeagueScreen profile={profile} leagueId={leagueId} onScopeChange={refreshScope} />}
        {section === 'ediciones' && <EditionScreen profile={profile} leagueId={leagueId} onScopeChange={refreshScope} />}
        {section === 'categorias' && <CategoryScreen profile={profile} editionId={editionId} />}
        {section === 'equipos' && <TeamScreen profile={profile} leagueId={leagueId} editionId={editionId} />}
        {section === 'jugadores' && (
          <PlayerScreen
            key={`${leagueId}-${editionId}-${equipoJugadores?.categoryId ?? ''}-${equipoJugadores?.teamId ?? ''}`}
            profile={profile}
            leagueId={leagueId}
            editionId={editionId}
            equipoInicial={equipoJugadores}
          />
        )}
        {section === 'inscripciones' && (
          <InscripcionScreen key={`${leagueId}-${editionId}`} profile={profile} leagueId={leagueId} editionId={editionId} />
        )}
        {section === 'refuerzos' && (
          <RefuerzoScreen
            key={`${leagueId}-${editionId}`}
            profile={profile}
            leagueId={leagueId}
            editionId={editionId}
            onAbrirPlantel={(equipo) => abrirSeccion('jugadores', equipo)}
          />
        )}
        {section === 'grupos' && <GruposScreen profile={profile} editionId={editionId} />}
        {section === 'fases' && <FasesScreen profile={profile} editionId={editionId} />}
        {section === 'clasificacion' && (
          <ClasificacionScreen
            key={editionId}
            editionId={editionId}
            leagueId={leagueId}
            leagueName={leagues.find((league) => league.id === leagueId)?.name ?? ''}
            hasLogo={leagues.some((league) => league.id === leagueId && league.hasLogo)}
            editionName={editions.find((edition) => edition.id === editionId)?.name ?? ''}
            logoVersion={scopeVersion}
          />
        )}
        {section === 'calendario' && <CalendarioScreen profile={profile} editionId={editionId} />}
        {section === 'horarios' && <HorariosScreen profile={profile} editionId={editionId} leagueId={leagueId} />}
        {section === 'resultados' && <ResultadosScreen key={editionId} profile={profile} editionId={editionId} leagueId={leagueId} />}
        {section === 'goles' && <GoleadoresScreen key={editionId} editionId={editionId} leagueId={leagueId} />}
        {section === 'tabla' && <TablaScreen key={editionId} editionId={editionId} leagueId={leagueId} />}
        {section === 'arbitros' && <ArbitrosScreen profile={profile} leagueId={leagueId} />}
        {section === 'catalogo-tarjetas' && <TarjetasCatalogoScreen profile={profile} leagueId={leagueId} />}
        {section === 'sanciones-jugador' && <SancionesJugadorScreen profile={profile} leagueId={leagueId} />}
        {section === 'suspensiones' && <SuspensionesScreen key={leagueId} profile={profile} leagueId={leagueId} />}
        {section === 'suspensiones-jugador' && <SuspensionesJugadorScreen key={editionId} editionId={editionId} leagueId={leagueId} />}
        {section === 'canchas' && <CanchasScreen profile={profile} leagueId={leagueId} />}
        {section === 'desempate-goleadores' && <DesempateGoleadoresScreen key={editionId} profile={profile} editionId={editionId} />}
        {section === 'desempate-equipos' && <DesempateEquiposScreen key={editionId} profile={profile} editionId={editionId} />}
        {section === 'publicaciones' && <PublicacionesScreen key={leagueId} profile={profile} leagueId={leagueId} editionId={editionId} />}
      </main>
      </div>
    </div>
    <footer className="pie">Created by Miguel Caicedo © All rights reserved</footer>
    </div>
  )
}

export default App
