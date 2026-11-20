import { useEffect, useState } from 'react'
import { api } from './api'

type Category = { id: string; name: string }
type Team = { id: string; name: string }

type Jornada = { id: string; numero: number; nombre: string; fecha: string | null }

type Jugador = {
  id: string
  alias: string
  numero: number
  equipo: string
  hasFoto: boolean
  rotacion: number
  pendientes: number
}

type Panel = { jornadaProxima: Jornada | null; jugadores: Jugador[] }

type Opcion = { motivo: string; etiqueta: string; jornadas: number; eligeInicio: boolean }

type Suspension = {
  id: string
  motivo: string
  etiqueta: string
  jornadaInicio: string
  jornadas: number
  origen: string
}

type Detalle = {
  jugadorId: string
  alias: string
  equipo: string
  opciones: Opcion[]
  jornadas: Jornada[]
  suspensiones: Suspension[]
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

function fecha(valor: string | null) {
  if (!valor) return ''
  const [anio, mes, dia] = valor.split('-')
  return `${dia}/${mes}/${anio}`
}

function textoJornada(jornada: Jornada) {
  const dia = fecha(jornada.fecha)
  return `Jornada ${jornada.numero}${dia ? ` · ${dia}` : ''}`
}

function Foto({ jugador }: { jugador: Jugador }) {
  if (jugador.hasFoto) {
    return (
      <img
        className="player-photo"
        alt=""
        src={`/api/players/${jugador.id}/photo`}
        style={{ transform: `rotate(${jugador.rotacion}deg)` }}
      />
    )
  }
  return <span className="player-photo vacio" aria-label="Sin foto" />
}

export function SuspensionesJugadorScreen({ editionId, leagueId }: { editionId: string; leagueId: string }) {
  const [ahora, setAhora] = useState(() => new Date())
  const [categories, setCategories] = useState<Category[]>([])
  const [teams, setTeams] = useState<Team[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [teamId, setTeamId] = useState('')
  const [alias, setAlias] = useState('')
  const [numero, setNumero] = useState('')
  const [panel, setPanel] = useState<Panel | null>(null)
  const [jugadorId, setJugadorId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const id = window.setInterval(() => setAhora(new Date()), 1000)
    return () => window.clearInterval(id)
  }, [])

  useEffect(() => {
    if (!editionId) return
    api<Category[]>(`/api/categories?editionId=${editionId}&estado=ACTIVO`)
      .then((items) => {
        setCategories(items)
        setCategoryId((actual) => actual || items[0]?.id || '')
      })
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  useEffect(() => {
    if (!leagueId || !editionId || !categoryId) {
      setTeams([])
      setTeamId('')
      return
    }
    api<Team[]>(`/api/teams?leagueId=${leagueId}&estado=ACTIVO&editionId=${editionId}&categoryId=${categoryId}`)
      .then((items) => {
        setTeams(items)
        setTeamId(items[0]?.id || '')
      })
      .catch((reason: Error) => setError(reason.message))
  }, [leagueId, editionId, categoryId])

  useEffect(() => {
    if (!editionId || !categoryId || !teamId) {
      setPanel(null)
      return
    }
    const params = new URLSearchParams({ editionId, categoryId, teamId })
    if (alias.trim()) params.set('alias', alias.trim())
    if (/^\d+$/.test(numero)) params.set('numero', numero)
    const espera = window.setTimeout(() => {
      api<Panel>(`/api/suspensiones-jugador?${params.toString()}`)
        .then((respuesta) => {
          setPanel(respuesta)
          setError(null)
        })
        .catch((reason: Error) => setError(reason.message))
    }, 250)
    return () => window.clearTimeout(espera)
  }, [editionId, categoryId, teamId, alias, numero, jugadorId])

  return (
    <section className="catalog">
      <p className="reloj">{reloj(ahora)}</p>
      {panel && (
        <p className="jornada-proxima">
          {panel.jornadaProxima
            ? `Jornada más próxima: ${textoJornada(panel.jornadaProxima)}`
            : 'No hay una jornada próxima'}
        </p>
      )}
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              {categories.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </label>
          <label>
            Equipo
            <select value={teamId} onChange={(event) => setTeamId(event.target.value)}>
              {teams.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </label>
          <label>
            Nombre
            <input value={alias} onChange={(event) => setAlias(event.target.value)} />
          </label>
          <label>
            Número de camisa
            <input value={numero} inputMode="numeric" onChange={(event) => setNumero(event.target.value)} />
          </label>
        </form>
      )}
      <table>
        <thead>
          <tr>
            <th></th>
            <th>Alias</th>
            <th>Equipo</th>
            <th>Suspensiones pendientes</th>
          </tr>
        </thead>
        <tbody>
          {(panel?.jugadores ?? []).map((jugador) => (
            <tr key={jugador.id} className="fila-jugador" onClick={() => setJugadorId(jugador.id)}>
              <td><Foto jugador={jugador} /></td>
              <td>{jugador.alias}</td>
              <td>{jugador.equipo}</td>
              <td>{jugador.pendientes}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {panel && panel.jugadores.length === 0 && <p>No hay jugadores con ese filtro.</p>}
      {jugadorId && categoryId && (
        <SuspensionModal
          jugadorId={jugadorId}
          categoryId={categoryId}
          jornadaProximaId={panel?.jornadaProxima?.id ?? ''}
          onClose={() => setJugadorId(null)}
        />
      )}
    </section>
  )
}

function SuspensionModal({
  jugadorId,
  categoryId,
  jornadaProximaId,
  onClose,
}: {
  jugadorId: string
  categoryId: string
  jornadaProximaId: string
  onClose: () => void
}) {
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const [motivo, setMotivo] = useState('')
  const [jornadas, setJornadas] = useState('1')
  const [jornadaInicioId, setJornadaInicioId] = useState(jornadaProximaId)
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  function cargar() {
    return api<Detalle>(`/api/suspensiones-jugador/jugador/${jugadorId}?categoryId=${categoryId}`).then((respuesta) => {
      setDetalle(respuesta)
      setMotivo((actual) => actual || respuesta.opciones[0]?.motivo || '')
      setJornadaInicioId((actual) => actual || jornadaProximaId || respuesta.jornadas[0]?.id || '')
    })
  }

  useEffect(() => {
    cargar().catch((reason: Error) => setError(reason.message))
  }, [jugadorId, categoryId])

  async function agregar() {
    if (!motivo) return
    const cantidad = Number(jornadas)
    if (motivo === 'SANCION' && (!Number.isInteger(cantidad) || cantidad < 1)) {
      setError('Indica las jornadas de la sanción')
      return
    }
    setOcupado(true)
    setError(null)
    try {
      const respuesta = await api<Detalle>('/api/suspensiones-jugador', {
        method: 'POST',
        body: JSON.stringify({
          jugadorId,
          categoryId,
          motivo,
          jornadas: motivo === 'SANCION' ? cantidad : null,
          jornadaInicioId: motivo === 'SANCION' ? jornadaInicioId : null,
        }),
      })
      setDetalle(respuesta)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo agregar la suspensión')
    } finally {
      setOcupado(false)
    }
  }

  async function quitar(id: string) {
    setOcupado(true)
    setError(null)
    try {
      await api(`/api/suspensiones-jugador/${id}`, { method: 'DELETE' })
      await cargar()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo quitar la suspensión')
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="modal-fondo" role="presentation">
      <section className="modal partido" role="dialog" aria-modal="true" aria-label="Suspensiones del jugador">
        <div className="partido-cabecera">
          <h2>{detalle ? `${detalle.alias} · ${detalle.equipo}` : 'Jugador'}</h2>
          <button type="button" className="ghost" onClick={onClose}>Cerrar</button>
        </div>
        {error && <p className="down">{error}</p>}
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Suspensión
            <select value={motivo} onChange={(event) => setMotivo(event.target.value)}>
              {(detalle?.opciones ?? []).map((item) => (
                <option key={item.motivo} value={item.motivo}>{item.etiqueta}</option>
              ))}
            </select>
          </label>
          {motivo === 'SANCION' && (
            <label>
              Jornadas
              <input
                type="number"
                min={1}
                value={jornadas}
                onChange={(event) => setJornadas(event.target.value)}
                required
              />
            </label>
          )}
          {motivo === 'SANCION' && (
            <label>
              Jornada de inicio
              <select value={jornadaInicioId} onChange={(event) => setJornadaInicioId(event.target.value)}>
                {(detalle?.jornadas ?? []).map((item) => (
                  <option key={item.id} value={item.id}>{textoJornada(item)}</option>
                ))}
              </select>
            </label>
          )}
          <button type="button" disabled={ocupado || !motivo} onClick={agregar}>Agregar</button>
        </form>
        <ul className="lista-detalle">
          {(detalle?.suspensiones ?? []).map((item) => (
            <li key={item.id}>
              <span>{item.etiqueta}</span>
              <span>{item.jornadaInicio}</span>
              <span>{item.motivo === 'FALTA_GRAVE' ? 'Todas' : `${item.jornadas} jornadas`}</span>
              <button type="button" className="ghost" disabled={ocupado} onClick={() => quitar(item.id)}>Quitar</button>
            </li>
          ))}
          {(detalle?.suspensiones.length ?? 0) === 0 && <li className="vacio">No tiene suspensiones.</li>}
        </ul>
      </section>
    </div>
  )
}
