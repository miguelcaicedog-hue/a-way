import { useEffect, useRef, useState } from 'react'
import { api } from './api'
import { FechaCampo } from './Fecha'
import { PartidoModal } from './PartidoModal'
import { CobroModal } from './CobroModal'
import { BotonDescargarImagen, IconoPdf } from './DescargarImagen'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Category = {
  id: string
  name: string
}

type Partido = {
  id: string
  categoryName: string
  jornadaNombre: string
  canchaNombre: string | null
  fecha: string | null
  hora: string | null
  local: string
  visita: string
  golesLocal: number | null
  golesVisita: number | null
  equiposDefinidos: boolean
  valorCobrado: number
  cerrada: boolean
  tieneGolesJugador: boolean
  tieneArbitro: boolean
  arbitroNoPito: boolean
  equiposCobrados: number
}

type Panel = {
  hoy: string
  partidos: Partido[]
  cerrada: boolean
}

function reloj(ahora: Date) {
  return ahora.toLocaleString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

function textoGol(valor: number | null) {
  return valor == null ? '' : String(valor)
}

function cobroRojo(partido: Partido, local: string, visita: string) {
  if (local === '' || visita === '' || Number(partido.valorCobrado) !== 0) {
    return false
  }
  return !(partido.arbitroNoPito && partido.equiposCobrados === 1)
}

function silbatoRojo(partido: Partido) {
  if (partido.arbitroNoPito || partido.golesLocal == null || partido.golesVisita == null) {
    return false
  }
  const sinGoleadores = partido.golesLocal + partido.golesVisita > 0 && !partido.tieneGolesJugador
  return sinGoleadores || !partido.tieneArbitro
}

export function ResultadosScreen({ profile, editionId, leagueId }: { profile: Profile; editionId: string; leagueId: string }) {
  const [ahora, setAhora] = useState(() => new Date())
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel | null>(null)
  const [goles, setGoles] = useState<Record<string, { local: string; visita: string }>>({})
  const [error, setError] = useState<string | null>(null)
  const [partidoDetalle, setPartidoDetalle] = useState<string | null>(null)
  const [partidoCobro, setPartidoCobro] = useState<string | null>(null)
  const [revisionCobro, setRevisionCobro] = useState(0)
  const [revisionDetalle, setRevisionDetalle] = useState(0)
  const [revisionCierre, setRevisionCierre] = useState(0)
  const [ocupado, setOcupado] = useState(false)
  const esperas = useRef<Record<string, number>>({})
  async function descargarAmonestados() {
    if (!editionId || !fecha) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, fecha })
      if (categoryId) params.set('categoryId', categoryId)
      const response = await fetch(`/api/reportes/tarjetas-suspensiones?${params.toString()}`, { credentials: 'include' })
      if (!response.ok) {
        const body = await response.json().catch(() => ({ error: 'No se pudo descargar el PDF' }))
        throw new Error(body.error ?? 'No se pudo descargar el PDF')
      }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `AMONESTADOS-Y-SUSPENDIDOS-${fecha}.pdf`
      enlace.click()
      URL.revokeObjectURL(url)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo descargar el PDF')
    } finally {
      setOcupado(false)
    }
  }

  async function descargarCobros() {
    if (!editionId || !fecha) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, fecha })
      if (categoryId) params.set('categoryId', categoryId)
      const response = await fetch(`/api/reportes/cobros?${params.toString()}`, { credentials: 'include' })
      if (!response.ok) {
        const body = await response.json().catch(() => ({ error: 'No se pudo descargar el PDF' }))
        throw new Error(body.error ?? 'No se pudo descargar el PDF')
      }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `COBROS-${fecha}.pdf`
      enlace.click()
      URL.revokeObjectURL(url)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo descargar el PDF')
    } finally {
      setOcupado(false)
    }
  }

  const puedeCualquierFecha = profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
  const esAsistente = profile.memberships.some((item) => item.role === 'ASISTENTE')

  useEffect(() => {
    const relojId = window.setInterval(() => setAhora(new Date()), 1000)
    return () => window.clearInterval(relojId)
  }, [])

  useEffect(() => {
    setCategoryId('')
    setFecha(null)
    setPanel(null)
  }, [editionId])

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then(setCategories)
      .catch((caught: Error) => setError(caught.message))
  }, [editionId])

  useEffect(() => {
    if (!editionId) return
    if (categoryId && !categories.some((item) => item.id === categoryId)) return
    let activo = true
    const params = new URLSearchParams({ editionId })
    if (categoryId) params.set('categoryId', categoryId)
    api<{ fecha: string | null }>(`/api/resultados/fecha-cercana?${params.toString()}`)
      .then((respuesta) => {
        if (activo) setFecha(respuesta.fecha)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, categories])

  useEffect(() => {
    if (!editionId || !fecha) {
      setPanel(null)
      return
    }
    const params = new URLSearchParams({ editionId, fecha })
    if (categoryId) params.set('categoryId', categoryId)
    let activo = true
    api<Panel>(`/api/resultados?${params.toString()}`)
      .then((siguiente) => {
        if (!activo) return
        setPanel(siguiente)
        setGoles((anterior) => {
          const nuevo = { ...anterior }
          for (const partido of siguiente.partidos) {
            if (esperas.current[partido.id]) continue
            nuevo[partido.id] = { local: textoGol(partido.golesLocal), visita: textoGol(partido.golesVisita) }
          }
          return nuevo
        })
        setError(null)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, fecha, revisionCobro, revisionCierre, revisionDetalle])

  function puedeEditar(partido: Partido) {
    if (!partido.equiposDefinidos || partido.cerrada) return false
    if (puedeCualquierFecha) return true
    return esAsistente && partido.fecha != null && panel?.hoy === partido.fecha
  }

  async function cambiarCierre(cerrar: boolean) {
    if (!editionId || !categoryId || !fecha) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, categoryId, fecha })
      await api(`/api/resultados/jornada/${cerrar ? 'cerrar' : 'abrir'}?${params.toString()}`, { method: 'PUT' })
      setRevisionCierre((valor) => valor + 1)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo cambiar el cierre de la jornada')
    } finally {
      setOcupado(false)
    }
  }

  async function mover(anterior: boolean) {
    if (!editionId) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId })
      if (categoryId) params.set('categoryId', categoryId)
      if (fecha) params.set('desde', fecha)
      const ruta = anterior ? 'fecha-anterior' : 'proxima-fecha'
      const respuesta = await api<{ fecha: string }>(`/api/resultados/${ruta}?${params.toString()}`)
      setFecha(respuesta.fecha)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No hay otra fecha de partidos')
    } finally {
      setOcupado(false)
    }
  }

  function cambiar(partido: Partido, lado: 'local' | 'visita', valor: string) {
    if (valor !== '' && !/^\d+$/.test(valor)) return
    const actual = goles[partido.id] ?? { local: textoGol(partido.golesLocal), visita: textoGol(partido.golesVisita) }
    const siguiente = { ...actual, [lado]: valor }
    setGoles((anterior) => ({ ...anterior, [partido.id]: siguiente }))
    window.clearTimeout(esperas.current[partido.id])
    esperas.current[partido.id] = window.setTimeout(() => {
      delete esperas.current[partido.id]
      guardar(partido.id, siguiente)
    }, 400)
  }

  async function guardar(id: string, marcador: { local: string; visita: string }) {
    setError(null)
    try {
      const guardado = await api<Partido>(`/api/resultados/partido/${id}`, {
        method: 'PUT',
        body: JSON.stringify({
          golesLocal: marcador.local === '' ? null : Number(marcador.local),
          golesVisita: marcador.visita === '' ? null : Number(marcador.visita),
        }),
      })
      if (esperas.current[id]) return
      setGoles((anterior) => ({
        ...anterior,
        [id]: { local: textoGol(guardado.golesLocal), visita: textoGol(guardado.golesVisita) },
      }))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo guardar el marcador')
    }
  }

  const porHora = new Map<string, Partido[]>()
  for (const partido of panel?.partidos ?? []) {
    const hora = partido.hora || 'Sin hora'
    const lista = porHora.get(hora) ?? []
    lista.push(partido)
    porHora.set(hora, lista)
  }
  const jornadaCerrada = Boolean(panel?.cerrada)
    || ((panel?.partidos.length ?? 0) > 0 && (panel?.partidos.every((partido) => partido.cerrada) ?? false))
  const partidosDelDia = panel?.partidos ?? []
  const todosConResultado = partidosDelDia.length > 0
    && partidosDelDia.every((partido) => partido.golesLocal != null && partido.golesVisita != null)
  const grupos = [...porHora.entries()].sort(([a], [b]) => {
    if (a === 'Sin hora') return 1
    if (b === 'Sin hora') return -1
    return a.localeCompare(b)
  })

  return (
    <section className="catalog">
      <div className="reloj-descargas">
        <p className="reloj">{reloj(ahora)}</p>
        <span className="reloj-botones">
          <BotonDescargarImagen leagueId={leagueId} tipo="RESULTADOS" fecha={fecha} etiqueta="DESCARGAR RESULTADOS" resultados={panel?.partidos ?? []} />
          <button type="button" className="con-icono descarga-pdf" disabled={!fecha || ocupado} onClick={() => void descargarAmonestados()}>
            <IconoPdf />
            AMONESTADOS Y SUSPENDIDOS
          </button>
          <button type="button" className="con-icono descarga-cobro" disabled={!fecha || ocupado} onClick={() => void descargarCobros()}>
            <svg className="icono-cobro" viewBox="0 0 24 24" aria-hidden="true">
              <path fillRule="evenodd" d="M5 4.6h14a2.2 2.2 0 0 1 2.2 2.2v10.4A2.2 2.2 0 0 1 19 19.4H5a2.2 2.2 0 0 1-2.2-2.2V6.8A2.2 2.2 0 0 1 5 4.6zm1.35 1.55v10.1h11.3V6.15H6.35z" />
              <text x="12" y="15.7" textAnchor="middle" fontSize="10.5" fontWeight="700" fontFamily="Arial, sans-serif">$</text>
            </svg>
            COBROS
          </button>
        </span>
      </div>
      {jornadaCerrada && <p className="jornada-cerrada">JORNADA CERRADA</p>}
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              <option value="">Todas</option>
              {categories.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </label>
          <label>
            Fecha
            <FechaCampo value={fecha} onChange={setFecha} ariaLabel="Fecha" />
          </label>
          <button type="button" disabled={ocupado} onClick={() => mover(true)}>FILTRAR FECHA ANTERIOR</button>
          <button type="button" disabled={ocupado} onClick={() => mover(false)}>FILTRAR PROXIMA FECHA</button>
        </form>
      )}
      {editionId && fecha && (panel?.partidos.length ?? 0) === 0 && <p>No hay partidos en esta fecha.</p>}
      {editionId && fecha && grupos.map(([hora, lista]) => (
        <section className="horario-dia" key={hora}>
          <h2>{hora}</h2>
          <table>
            <thead>
              <tr>
                <th>Cancha</th>
                <th>Local</th>
                <th>Goles</th>
                <th>Visita</th>
                <th>Goles</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {lista.map((partido) => {
                const marcador = goles[partido.id] ?? { local: textoGol(partido.golesLocal), visita: textoGol(partido.golesVisita) }
                const edita = puedeEditar(partido)
                return (
                  <tr key={partido.id}>
                    <td>{partido.canchaNombre ?? ''}</td>
                    <td>{partido.local}</td>
                    <td>
                      <input
                        className="marcador"
                        inputMode="numeric"
                        value={marcador.local}
                        disabled={!edita}
                        onChange={(event) => cambiar(partido, 'local', event.target.value)}
                      />
                    </td>
                    <td>{partido.visita}</td>
                    <td>
                      <input
                        className="marcador"
                        inputMode="numeric"
                        value={marcador.visita}
                        disabled={!edita}
                        onChange={(event) => cambiar(partido, 'visita', event.target.value)}
                      />
                    </td>
                    <td className="acciones-partido">
                      <button
                        type="button"
                        className={silbatoRojo(partido) ? 'silbato sin-cobro' : 'silbato'}
                        aria-label="Detalle del partido"
                        onClick={() => setPartidoDetalle(partido.id)}
                      >
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path d="M4 10h6l2-2h3a4 4 0 1 1 0 8h-1.2l-1.3 2.2a2 2 0 0 1-3.4 0L8 16H4a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2zm13 1.2a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6z" />
                        </svg>
                      </button>
                      {cobroRojo(partido, marcador.local, marcador.visita) ? (
                        <span className="aviso-sin-cobro">
                          <button type="button" className="silbato sin-cobro" aria-label="No se ha cobrado ningún valor" onClick={() => setPartidoCobro(partido.id)}>
                            <svg viewBox="0 0 24 24" aria-hidden="true">
                              <path fillRule="evenodd" d="M5 4.6h14a2.2 2.2 0 0 1 2.2 2.2v10.4A2.2 2.2 0 0 1 19 19.4H5a2.2 2.2 0 0 1-2.2-2.2V6.8A2.2 2.2 0 0 1 5 4.6zm1.35 1.55v10.1h11.3V6.15H6.35z" />
                              <text x="12" y="15.7" textAnchor="middle" fontSize="10.5" fontWeight="700" fontFamily="Arial, sans-serif">$</text>
                            </svg>
                          </button>
                          <span role="tooltip">No se ha cobrado ningún valor</span>
                        </span>
                      ) : (
                        <button type="button" className="silbato" aria-label="Cobro del partido" onClick={() => setPartidoCobro(partido.id)}>
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path fillRule="evenodd" d="M5 4.6h14a2.2 2.2 0 0 1 2.2 2.2v10.4A2.2 2.2 0 0 1 19 19.4H5a2.2 2.2 0 0 1-2.2-2.2V6.8A2.2 2.2 0 0 1 5 4.6zm1.35 1.55v10.1h11.3V6.15H6.35z" />
                            <text x="12" y="15.7" textAnchor="middle" fontSize="10.5" fontWeight="700" fontFamily="Arial, sans-serif">$</text>
                          </svg>
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </section>
      ))}
      {partidoDetalle && (
        <PartidoModal
          partidoId={partidoDetalle}
          onClose={() => {
            setPartidoDetalle(null)
            setRevisionDetalle((valor) => valor + 1)
          }}
        />
      )}
      {editionId && (
        <div className="cierre-jornada">
          <button
            type="button"
            disabled={!puedeCualquierFecha || !categoryId || !fecha || ocupado || (panel?.partidos.length ?? 0) === 0 || Boolean(panel?.cerrada)}
            onClick={() => cambiarCierre(true)}
          >
            Cerrar jornada
          </button>
          <button
            type="button"
            disabled={!puedeCualquierFecha || !categoryId || !fecha || ocupado || !panel?.cerrada}
            onClick={() => cambiarCierre(false)}
          >
            Abrir jornada
          </button>
        </div>
      )}
      {editionId && !categoryId && !jornadaCerrada && todosConResultado && (
        <p className="aviso-cierre">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M12 3.2 1.8 21h20.4L12 3.2zm0 6.2c.5 0 .9.4.9.9v4.6a.9.9 0 0 1-1.8 0v-4.6c0-.5.4-.9.9-.9zm0 9.1a1.15 1.15 0 1 1 0-2.3 1.15 1.15 0 0 1 0 2.3z" />
          </svg>
          Selecciona una categoría para cerrar la jornada.
        </p>
      )}
      {partidoCobro && (
        <CobroModal
          partidoId={partidoCobro}
          onClose={() => {
            setPartidoCobro(null)
            setRevisionCobro((valor) => valor + 1)
          }}
        />
      )}
    </section>
  )
}
