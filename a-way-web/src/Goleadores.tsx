import { useEffect, useState } from 'react'
import { api } from './api'
import { aDia, FechaCampo } from './Fecha'
import { BotonDescargarImagen } from './DescargarImagen'

type Category = {
  id: string
  name: string
}

type Equipo = {
  id: string
  nombre: string
}

type Detalle = {
  jornada: string
  fecha: string | null
  rival: string
  cantidad: number
}

type Goleador = {
  jugadorId: string
  alias: string
  nombre: string
  equipoId: string
  equipo: string
  goles: number
  hasPhoto: boolean
  rotationDegrees: number
  detalle: Detalle[]
}

type Panel = {
  fecha: string
  equipos: Equipo[]
  goleadores: Goleador[]
}

const TAMANO = 10

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

function etiqueta(goleador: Goleador) {
  return goleador.alias.trim() || goleador.nombre
}

function Foto({ goleador }: { goleador: Goleador }) {
  if (goleador.hasPhoto) {
    return (
      <img
        className="player-photo"
        alt=""
        src={`/api/players/${goleador.jugadorId}/photo`}
        style={{ transform: `rotate(${goleador.rotationDegrees}deg)` }}
      />
    )
  }
  return <span className="player-photo vacio" aria-label="Sin foto" />
}

export function GoleadoresScreen({ editionId, leagueId }: { editionId: string; leagueId: string }) {
  const [ahora, setAhora] = useState(() => new Date())
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [teamId, setTeamId] = useState('')
  const [panel, setPanel] = useState<Panel | null>(null)
  const [pagina, setPagina] = useState(1)
  const [detalle, setDetalle] = useState<Goleador | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  useEffect(() => {
    const relojId = window.setInterval(() => setAhora(new Date()), 1000)
    return () => window.clearInterval(relojId)
  }, [])

  useEffect(() => {
    setCategories([])
    setCategoryId('')
    setFecha(null)
    setTeamId('')
    setPanel(null)
    setPagina(1)
  }, [editionId])

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      return
    }
    let activo = true
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        if (!activo) return
        setCategories(items)
        setCategoryId(items[0]?.id ?? '')
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId])

  useEffect(() => {
    if (!editionId || !categoryId) return
    if (!categories.some((item) => item.id === categoryId)) return
    let activo = true
    const params = new URLSearchParams({ editionId, categoryId })
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
    setPagina(1)
  }, [categoryId, fecha, teamId])

  useEffect(() => {
    if (!editionId || !categoryId || !fecha) {
      setPanel(null)
      return
    }
    const params = new URLSearchParams({ editionId, categoryId, fecha })
    if (teamId) params.set('teamId', teamId)
    let activo = true
    function cargar() {
      api<Panel>(`/api/goleadores?${params.toString()}`)
        .then((siguiente) => {
          if (!activo) return
          setPanel(siguiente)
          setError(null)
        })
        .catch((caught: Error) => {
          if (activo) setError(caught.message)
        })
    }
    cargar()
    function alVolver() {
      if (document.visibilityState === 'visible') cargar()
    }
    document.addEventListener('visibilitychange', alVolver)
    return () => {
      activo = false
      document.removeEventListener('visibilitychange', alVolver)
    }
  }, [editionId, categoryId, fecha, teamId])

  async function mover(anterior: boolean) {
    if (!editionId || !categoryId) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, categoryId })
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

  const goleadores = panel?.goleadores ?? []
  const paginas = Math.max(1, Math.ceil(goleadores.length / TAMANO))
  const paginaVisible = Math.min(pagina, paginas)
  const desde = (paginaVisible - 1) * TAMANO
  const paginaActual = goleadores.slice(desde, desde + TAMANO)
  const equipos = panel?.equipos ?? []

  return (
    <section className="catalog">
      <p className="reloj">{reloj(ahora)}</p>
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && categories.length === 0 && <p>Esta edición no tiene categorías.</p>}
      {editionId && categories.length > 0 && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setTeamId('') }} required>
              {categories.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </label>
          <label>
            Fecha
            <FechaCampo value={fecha} onChange={setFecha} ariaLabel="Fecha" />
          </label>
          <label>
            Equipo
            <select value={teamId} onChange={(event) => setTeamId(event.target.value)}>
              <option value="">Todos</option>
              {equipos.map((item) => (
                <option key={item.id} value={item.id}>{item.nombre}</option>
              ))}
            </select>
          </label>
          <button type="button" disabled={ocupado} onClick={() => mover(true)}>FILTRAR FECHA ANTERIOR</button>
          <button type="button" disabled={ocupado} onClick={() => mover(false)}>FILTRAR PROXIMA FECHA</button>
          <BotonDescargarImagen leagueId={leagueId} tipo="GOLEADORES" fecha={fecha} goleadores={goleadores} />
        </form>
      )}
      {editionId && categoryId && !fecha && <p>No hay fechas de partidos en esta categoría.</p>}
      {editionId && fecha && panel && goleadores.length === 0 && <p>No hay goleadores en este corte.</p>}
      {paginaActual.length > 0 && (
        <section className="goleadores">
          <table>
            <thead>
              <tr>
                <th>Foto</th>
                <th>Alias</th>
                <th>Equipo</th>
                <th>Goles</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {paginaActual.map((goleador) => (
                <tr key={goleador.jugadorId}>
                  <td><Foto goleador={goleador} /></td>
                  <td>{etiqueta(goleador)}</td>
                  <td>{goleador.equipo}</td>
                  <td className="numero">{goleador.goles}</td>
                  <td>
                    <button type="button" className="lupa" aria-label="Detalle de goles" onClick={() => setDetalle(goleador)}>
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M10.5 3a7.5 7.5 0 0 1 5.9 12.1l4 4a1 1 0 0 1-1.4 1.4l-4-4A7.5 7.5 0 1 1 10.5 3zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11z" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {goleadores.length > TAMANO && (
            <div className="paginacion">
              <button type="button" disabled={paginaVisible <= 1} onClick={() => setPagina(paginaVisible - 1)}>Anterior</button>
              <span>Página {paginaVisible} de {paginas}</span>
              <button type="button" disabled={paginaVisible >= paginas} onClick={() => setPagina(paginaVisible + 1)}>Siguiente</button>
            </div>
          )}
        </section>
      )}
      {detalle && (
        <div className="modal-fondo" role="presentation" onClick={() => setDetalle(null)}>
          <div className="modal" role="dialog" aria-modal="true" aria-labelledby="detalle-goles-titulo" onClick={(event) => event.stopPropagation()}>
            <h2 id="detalle-goles-titulo">{etiqueta(detalle)}</h2>
            <p>{detalle.equipo} · {detalle.goles} goles</p>
            <table>
              <thead>
                <tr>
                  <th>Jornada</th>
                  <th>Fecha</th>
                  <th>Rival</th>
                  <th>Goles</th>
                </tr>
              </thead>
              <tbody>
                {detalle.detalle.map((fila, indice) => (
                  <tr key={`${fila.jornada}-${fila.fecha}-${indice}`}>
                    <td>{fila.jornada}</td>
                    <td>{aDia(fila.fecha)}</td>
                    <td>{fila.rival}</td>
                    <td className="numero">{fila.cantidad}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button type="button" onClick={() => setDetalle(null)}>Cerrar</button>
          </div>
        </div>
      )}
    </section>
  )
}
