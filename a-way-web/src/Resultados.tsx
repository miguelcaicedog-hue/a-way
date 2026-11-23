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

type LineaPago = {
  numero: number
  hora: string
  categoria: string
  jornada: string
  partido: string
  pito: boolean
  pago: number
}

type VistaPago = {
  liga: string
  subtitulo: string
  generado: string
  arbitros: { nombre: string; lineas: LineaPago[]; total: number }[]
  resumen: { numero: number; nombre: string; partidos: number; total: number }[]
  totalPartidos: number
  totalPago: number
}

type FilaCobro = {
  numero: number
  hora: string
  categoria: string
  jornada: string
  partido: string
  arbitraje: number
  tarjetas: number
  suspensiones: number
  total: number
  efectivo: number
  transferencia: number
  totalCobrado: number
}

type VistaCobros = {
  filas: FilaCobro[]
  arbitraje: number
  tarjetas: number
  suspensiones: number
  total: number
  efectivo: number
  transferencia: number
  totalCobrado: number
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

function dinero(valor: number) {
  return Number(valor ?? 0).toFixed(2)
}

type MontosCobro = {
  arbitraje: number
  tarjetas: number
  suspensiones: number
  total: number
  efectivo: number
  transferencia: number
  totalCobrado: number
}

function sumarCobros(filas: FilaCobro[]): MontosCobro {
  return filas.reduce((acc, fila) => ({
    arbitraje: acc.arbitraje + Number(fila.arbitraje),
    tarjetas: acc.tarjetas + Number(fila.tarjetas),
    suspensiones: acc.suspensiones + Number(fila.suspensiones),
    total: acc.total + Number(fila.total),
    efectivo: acc.efectivo + Number(fila.efectivo),
    transferencia: acc.transferencia + Number(fila.transferencia),
    totalCobrado: acc.totalCobrado + Number(fila.totalCobrado),
  }), { arbitraje: 0, tarjetas: 0, suspensiones: 0, total: 0, efectivo: 0, transferencia: 0, totalCobrado: 0 })
}

function agruparCobros(filas: FilaCobro[]) {
  const categorias: { nombre: string; jornadas: { nombre: string; filas: FilaCobro[] }[] }[] = []
  for (const fila of filas) {
    let categoria = categorias.find((item) => item.nombre === fila.categoria)
    if (!categoria) {
      categoria = { nombre: fila.categoria, jornadas: [] }
      categorias.push(categoria)
    }
    let jornada = categoria.jornadas.find((item) => item.nombre === fila.jornada)
    if (!jornada) {
      jornada = { nombre: fila.jornada, filas: [] }
      categoria.jornadas.push(jornada)
    }
    jornada.filas.push(fila)
  }
  return categorias
}

function MontosCobroVista({ montos }: { montos: MontosCobro }) {
  const campos: [string, number, boolean][] = [
    ['Arbitraje', montos.arbitraje, false],
    ['Tarjetas', montos.tarjetas, false],
    ['Suspensiones', montos.suspensiones, false],
    ['Total', montos.total, false],
    ['Cobro real', montos.efectivo, true],
    ['Transferencia', montos.transferencia, true],
    ['Total cobrado', montos.totalCobrado, true],
  ]
  return (
    <span className="cobro-montos">
      {campos.map(([etiqueta, valor, cobrado]) => (
        <span key={etiqueta} className={cobrado ? 'cobrado' : undefined}>
          <small>{etiqueta}</small>
          {dinero(valor)}
        </span>
      ))}
    </span>
  )
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
  const [menuPago, setMenuPago] = useState(false)
  const [vistaPago, setVistaPago] = useState<VistaPago | null>(null)
  const [menuCobro, setMenuCobro] = useState(false)
  const [vistaCobros, setVistaCobros] = useState<VistaCobros | null>(null)
  const menuPagoRef = useRef<HTMLSpanElement>(null)
  const menuCobroRef = useRef<HTMLSpanElement>(null)
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

  async function abrirVistaCobros() {
    if (!editionId || !fecha) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, fecha })
      if (categoryId) params.set('categoryId', categoryId)
      setVistaCobros(await api<VistaCobros>(`/api/reportes/cobros/vista?${params.toString()}`))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo abrir la vista previa')
    } finally {
      setOcupado(false)
    }
  }

  async function descargarPagoArbitros() {
    if (!editionId || !fecha) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, fecha })
      if (categoryId) params.set('categoryId', categoryId)
      const response = await fetch(`/api/reportes/pago-arbitros?${params.toString()}`, { credentials: 'include' })
      if (!response.ok) {
        const body = await response.json().catch(() => ({ error: 'No se pudo descargar el PDF' }))
        throw new Error(body.error ?? 'No se pudo descargar el PDF')
      }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `PAGO-ARBITROS-${fecha}.pdf`
      enlace.click()
      URL.revokeObjectURL(url)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo descargar el PDF')
    } finally {
      setOcupado(false)
    }
  }

  async function abrirVistaPago() {
    if (!editionId || !fecha) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, fecha })
      if (categoryId) params.set('categoryId', categoryId)
      setVistaPago(await api<VistaPago>(`/api/reportes/pago-arbitros/vista?${params.toString()}`))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo abrir la vista previa')
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
    if (!menuPago && !menuCobro) return
    function cerrar(event: MouseEvent) {
      const destino = event.target as Node
      if (menuPago && !menuPagoRef.current?.contains(destino)) {
        setMenuPago(false)
      }
      if (menuCobro && !menuCobroRef.current?.contains(destino)) {
        setMenuCobro(false)
      }
    }
    document.addEventListener('mousedown', cerrar)
    return () => document.removeEventListener('mousedown', cerrar)
  }, [menuPago, menuCobro])

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
          <span className="menu-descarga" ref={menuCobroRef}>
            <button type="button" className="con-icono descarga-cobro" disabled={!fecha || ocupado} aria-expanded={menuCobro} onClick={() => setMenuCobro((abierto) => !abierto)}>
              <svg className="icono-cobro" viewBox="0 0 24 24" aria-hidden="true">
                <path fillRule="evenodd" d="M5 4.6h14a2.2 2.2 0 0 1 2.2 2.2v10.4A2.2 2.2 0 0 1 19 19.4H5a2.2 2.2 0 0 1-2.2-2.2V6.8A2.2 2.2 0 0 1 5 4.6zm1.35 1.55v10.1h11.3V6.15H6.35z" />
                <text x="12" y="15.7" textAnchor="middle" fontSize="10.5" fontWeight="700" fontFamily="Arial, sans-serif">$</text>
              </svg>
              COBROS
            </button>
            {menuCobro && (
              <span className="menu-descarga-opciones" role="menu">
                <button type="button" role="menuitem" onClick={() => { setMenuCobro(false); void abrirVistaCobros() }}>Vista previa</button>
                <button type="button" role="menuitem" onClick={() => { setMenuCobro(false); void descargarCobros() }}>Descargar PDF</button>
              </span>
            )}
          </span>
          <span className="menu-descarga" ref={menuPagoRef}>
            <button type="button" className="con-icono descarga-arbitro" disabled={!fecha || ocupado} aria-expanded={menuPago} onClick={() => setMenuPago((abierto) => !abierto)}>
              <svg className="icono-arbitro" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 10h6l2-2h3a4 4 0 1 1 0 8h-1.2l-1.3 2.2a2 2 0 0 1-3.4 0L8 16H4a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2zm13 1.2a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6z" />
              </svg>
              PAGO ÁRBITROS
            </button>
            {menuPago && (
              <span className="menu-descarga-opciones" role="menu">
                <button type="button" role="menuitem" onClick={() => { setMenuPago(false); void abrirVistaPago() }}>Vista previa</button>
                <button type="button" role="menuitem" onClick={() => { setMenuPago(false); void descargarPagoArbitros() }}>Descargar PDF</button>
              </span>
            )}
          </span>
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
      {vistaCobros && (
        <div className="modal-fondo" role="presentation" onClick={() => setVistaCobros(null)}>
          <section className="modal cobros-resumen" role="dialog" aria-modal="true" aria-label="Cobros" onClick={(event) => event.stopPropagation()}>
            <div className="partido-cabecera">
              <button type="button" className="ghost" onClick={() => setVistaCobros(null)}>Cerrar</button>
            </div>
            {vistaCobros.filas.length === 0 && <p className="vacio-reporte">No hay partidos en esta fecha.</p>}
            {vistaCobros.filas.length > 0 && (
              <>
                <div className="cobros-total">
                  <strong>Total</strong>
                  <MontosCobroVista montos={{
                    arbitraje: vistaCobros.arbitraje,
                    tarjetas: vistaCobros.tarjetas,
                    suspensiones: vistaCobros.suspensiones,
                    total: vistaCobros.total,
                    efectivo: vistaCobros.efectivo,
                    transferencia: vistaCobros.transferencia,
                    totalCobrado: vistaCobros.totalCobrado,
                  }} />
                </div>
                {agruparCobros(vistaCobros.filas).map((categoria) => {
                  const filasCategoria = categoria.jornadas.flatMap((jornada) => jornada.filas)
                  return (
                    <details key={categoria.nombre} className="cobro-grupo">
                      <summary>
                        <span>{categoria.nombre}</span>
                        <MontosCobroVista montos={sumarCobros(filasCategoria)} />
                      </summary>
                      {categoria.jornadas.map((jornada) => (
                        <details key={jornada.nombre} className="cobro-grupo">
                          <summary>
                            <span>{jornada.nombre}</span>
                            <MontosCobroVista montos={sumarCobros(jornada.filas)} />
                          </summary>
                          <table>
                            <thead>
                              <tr>
                                <th className="numero">N°</th>
                                <th>Hora</th>
                                <th>Partido</th>
                                <th className="monto">Arbitraje</th>
                                <th className="monto">Tarjetas</th>
                                <th className="monto">Suspensiones</th>
                                <th className="monto">Total</th>
                                <th className="monto cobrado">Cobro real</th>
                                <th className="monto cobrado">Transferencia</th>
                                <th className="monto cobrado">Total</th>
                              </tr>
                            </thead>
                            <tbody>
                              {jornada.filas.map((fila, indice) => (
                                <tr key={fila.numero}>
                                  <td className="numero">{indice + 1}</td>
                                  <td>{fila.hora}</td>
                                  <td>{fila.partido}</td>
                                  <td className="monto">{dinero(fila.arbitraje)}</td>
                                  <td className="monto">{dinero(fila.tarjetas)}</td>
                                  <td className="monto">{dinero(fila.suspensiones)}</td>
                                  <td className="monto">{dinero(fila.total)}</td>
                                  <td className="monto cobrado">{dinero(fila.efectivo)}</td>
                                  <td className="monto cobrado">{dinero(fila.transferencia)}</td>
                                  <td className="monto cobrado">{dinero(fila.totalCobrado)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </details>
                      ))}
                    </details>
                  )
                })}
              </>
            )}
          </section>
        </div>
      )}
      {vistaPago && (
        <div className="modal-fondo" role="presentation" onClick={() => setVistaPago(null)}>
          <section className="modal pago-arbitros" role="dialog" aria-modal="true" aria-label="Pago árbitros" onClick={(event) => event.stopPropagation()}>
            <div className="partido-cabecera">
              <button type="button" className="ghost" onClick={() => setVistaPago(null)}>Cerrar</button>
            </div>
            {vistaPago.arbitros.length === 0 && <p className="vacio-reporte">No hay árbitros asignados.</p>}
            {vistaPago.arbitros.length > 0 && (
              <div>
                <h3>RESUMEN</h3>
                <table>
                  <thead>
                    <tr>
                      <th className="numero">N°</th>
                      <th>Árbitro</th>
                      <th className="monto">Partidos</th>
                      <th className="monto">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {vistaPago.resumen.map((fila) => (
                      <tr key={fila.numero}>
                        <td className="numero">{fila.numero}</td>
                        <td>{fila.nombre}</td>
                        <td className="monto">{fila.partidos}</td>
                        <td className="monto">{dinero(fila.total)}</td>
                      </tr>
                    ))}
                    <tr className="total">
                      <td colSpan={2}>Total</td>
                      <td className="monto">{vistaPago.totalPartidos}</td>
                      <td className="monto">{dinero(vistaPago.totalPago)}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            )}
            {vistaPago.arbitros.map((arbitro) => (
              <details key={arbitro.nombre} className="pago-arbitro">
                <summary>
                  <span>{arbitro.nombre}</span>
                  <span className="monto">{dinero(arbitro.total)}</span>
                </summary>
                <table>
                  <thead>
                    <tr>
                      <th className="numero">N°</th>
                      <th>Hora</th>
                      <th>Categoría</th>
                      <th>Jornada</th>
                      <th>Partido</th>
                      <th>Pitó</th>
                      <th className="monto">Pago</th>
                    </tr>
                  </thead>
                  <tbody>
                    {arbitro.lineas.map((linea) => (
                      <tr key={linea.numero}>
                        <td className="numero">{linea.numero}</td>
                        <td>{linea.hora}</td>
                        <td>{linea.categoria}</td>
                        <td>{linea.jornada}</td>
                        <td>{linea.partido}</td>
                        <td>{linea.pito ? 'Sí' : 'No'}</td>
                        <td className="monto">{dinero(linea.pago)}</td>
                      </tr>
                    ))}
                    <tr className="total">
                      <td colSpan={6}>Total</td>
                      <td className="monto">{dinero(arbitro.total)}</td>
                    </tr>
                  </tbody>
                </table>
              </details>
            ))}
          </section>
        </div>
      )}
    </section>
  )
}
