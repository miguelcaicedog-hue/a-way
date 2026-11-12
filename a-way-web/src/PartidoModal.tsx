import { useEffect, useState } from 'react'
import { api } from './api'

type Arbitro = {
  id: string
  nombre: string
  hasFoto: boolean
}

type Asignado = {
  arbitroId: string
  pito: boolean
}

type Equipo = {
  id: string
  nombre: string
} | null

type Detalle = {
  local: string
  visita: string
  equipoLocal: Equipo
  equipoVisita: Equipo
  editable: boolean
  arbitros: Arbitro[]
  asignados: Asignado[]
  goles: { jugadorId: string; cantidad: number }[]
}

type Jugador = {
  id: string
  numero: number
  alias: string
  hasFoto: boolean
  rotacion: number
}

function FotoArbitro({ id, hasFoto }: { id: string; hasFoto: boolean }) {
  if (hasFoto) {
    return <img className="arbitro-foto" alt="" src={`/api/arbitros/${id}/foto`} />
  }
  return <span className="arbitro-foto vacio" aria-label="Sin foto" />
}

function FotoJugador({ jugador }: { jugador: Jugador }) {
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

export function PartidoModal({ partidoId, onClose }: { partidoId: string; onClose: () => void }) {
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const [pestana, setPestana] = useState<'arbitros' | 'goleadores' | 'tarjetas' | 'sanciones'>('arbitros')
  const [nombreArbitro, setNombreArbitro] = useState('')
  const [asignados, setAsignados] = useState<Asignado[]>([])
  const [equipoId, setEquipoId] = useState('')
  const [numero, setNumero] = useState('')
  const [nombreJugador, setNombreJugador] = useState('')
  const [jugadores, setJugadores] = useState<Jugador[]>([])
  const [goles, setGoles] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [grabado, setGrabado] = useState(false)

  function golesDe(respuesta: Detalle) {
    const cargados: Record<string, string> = {}
    for (const gol of respuesta.goles) cargados[gol.jugadorId] = String(gol.cantidad)
    return cargados
  }

  useEffect(() => {
    let activo = true
    api<Detalle>(`/api/resultados/partido/${partidoId}/detalle`)
      .then((respuesta) => {
        if (!activo) return
        setDetalle(respuesta)
        setAsignados(respuesta.asignados)
        setGoles(golesDe(respuesta))
        setEquipoId(respuesta.equipoLocal?.id ?? respuesta.equipoVisita?.id ?? '')
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [partidoId])

  useEffect(() => {
    if (!equipoId) {
      setJugadores([])
      return
    }
    const params = new URLSearchParams({ teamId: equipoId })
    if (numero.trim()) params.set('numero', numero.trim())
    if (nombreJugador.trim()) params.set('nombre', nombreJugador.trim())
    let activo = true
    api<Jugador[]>(`/api/resultados/partido/${partidoId}/jugadores?${params.toString()}`)
      .then((lista) => {
        if (activo) setJugadores(lista)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [partidoId, equipoId, numero, nombreJugador])

  function elegir(arbitro: Arbitro) {
    if (!detalle?.editable || asignados.some((item) => item.arbitroId === arbitro.id)) return
    setGrabado(false)
    setAsignados([...asignados, { arbitroId: arbitro.id, pito: true }])
  }

  function quitar(arbitroId: string) {
    setGrabado(false)
    setAsignados(asignados.filter((item) => item.arbitroId !== arbitroId))
  }

  function marcarPito(arbitroId: string, pito: boolean) {
    setGrabado(false)
    setAsignados(asignados.map((item) => (item.arbitroId === arbitroId ? { ...item, pito } : item)))
  }

  async function grabar() {
    if (!detalle?.editable) return
    setOcupado(true)
    setError(null)
    try {
      const respuesta = await api<Detalle>(`/api/resultados/partido/${partidoId}/detalle`, {
        method: 'PUT',
        body: JSON.stringify({
          arbitros: asignados,
          goles: Object.entries(goles)
            .filter(([, cantidad]) => cantidad !== '' && Number(cantidad) > 0)
            .map(([jugadorId, cantidad]) => ({ jugadorId, cantidad: Number(cantidad) })),
        }),
      })
      setDetalle(respuesta)
      setAsignados(respuesta.asignados)
      setGoles(golesDe(respuesta))
      setGrabado(true)
    } catch (caught) {
      setGrabado(false)
      setError(caught instanceof Error ? caught.message : 'No se pudo grabar el partido')
    } finally {
      setOcupado(false)
    }
  }

  const visibles = (detalle?.arbitros ?? []).filter((arbitro) =>
    arbitro.nombre.toLowerCase().includes(nombreArbitro.trim().toLowerCase()))
  const equipos = [detalle?.equipoLocal, detalle?.equipoVisita].filter((equipo): equipo is { id: string; nombre: string } => Boolean(equipo))

  return (
    <div className="modal-fondo" role="presentation">
      <section className="modal partido" role="dialog" aria-modal="true" aria-label="Detalle del partido">
        <div className="partido-cabecera">
          <h2>
            {detalle ? (
              <>
                <span className="equipo-local">{detalle.local}</span>
                <span className="vs">VS</span>
                <span className="equipo-visita">{detalle.visita}</span>
              </>
            ) : 'Partido'}
          </h2>
          <div className="actions">
            {grabado && <span className="aviso-grabado">Grabado</span>}
            <button type="button" className="ghost" onClick={onClose}>Cerrar</button>
            {detalle?.editable && <button type="button" disabled={ocupado} onClick={grabar}>Grabar</button>}
          </div>
        </div>
        {error && <p className="down">{error}</p>}
        <div className="partido-cuerpo">
        <div className="pestanas">
          <button type="button" className={pestana === 'arbitros' ? 'activa' : ''} onClick={() => setPestana('arbitros')}>Árbitros</button>
          <button type="button" className={pestana === 'goleadores' ? 'activa' : ''} onClick={() => setPestana('goleadores')}>Goleadores</button>
          <button type="button" className={pestana === 'tarjetas' ? 'activa' : ''} onClick={() => setPestana('tarjetas')}>Tarjetas</button>
          <button type="button" className={pestana === 'sanciones' ? 'activa' : ''} onClick={() => setPestana('sanciones')}>Sanciones</button>
        </div>
        {pestana === 'arbitros' && (
          <div>
            <label>
              Nombre
              <input value={nombreArbitro} onChange={(event) => setNombreArbitro(event.target.value)} />
            </label>
            <ul className="lista-detalle">
              {visibles.map((arbitro) => {
                const asignado = asignados.find((item) => item.arbitroId === arbitro.id)
                return (
                  <li key={arbitro.id}>
                    <FotoArbitro id={arbitro.id} hasFoto={arbitro.hasFoto} />
                    <span>{arbitro.nombre}</span>
                    {asignado ? (
                      <>
                        <label className="pito">
                          <input
                            type="checkbox"
                            checked={asignado.pito}
                            disabled={!detalle?.editable}
                            onChange={(event) => marcarPito(arbitro.id, event.target.checked)}
                          />
                          Pitó
                        </label>
                        {detalle?.editable && <button type="button" className="ghost" onClick={() => quitar(arbitro.id)}>Quitar</button>}
                      </>
                    ) : (
                      detalle?.editable && <button type="button" onClick={() => elegir(arbitro)}>Elegir</button>
                    )}
                  </li>
                )
              })}
              {visibles.length === 0 && <li>No hay árbitros.</li>}
            </ul>
          </div>
        )}
        {pestana === 'goleadores' && (
          <div>
            <form className="filtros" onSubmit={(event) => event.preventDefault()}>
              <label>
                Equipo
                <select value={equipoId} onChange={(event) => setEquipoId(event.target.value)}>
                  {equipos.map((equipo) => (
                    <option key={equipo.id} value={equipo.id}>{equipo.nombre}</option>
                  ))}
                </select>
              </label>
              <label>
                Número de camisa
                <input value={numero} onChange={(event) => setNumero(event.target.value)} />
              </label>
              <label>
                Nombre
                <input value={nombreJugador} onChange={(event) => setNombreJugador(event.target.value)} />
              </label>
            </form>
            <ul className="lista-detalle">
              {jugadores.map((jugador) => (
                <li key={jugador.id}>
                  <FotoJugador jugador={jugador} />
                  <span>{jugador.numero}</span>
                  <span>{jugador.alias}</span>
                  <input
                    className="marcador"
                    inputMode="numeric"
                    aria-label="Goles"
                    value={goles[jugador.id] ?? ''}
                    disabled={!detalle?.editable}
                    onChange={(event) => {
                      const valor = event.target.value
                      if (valor !== '' && !/^\d+$/.test(valor)) return
                      setGrabado(false)
                      setGoles({ ...goles, [jugador.id]: valor })
                    }}
                  />
                </li>
              ))}
              {jugadores.length === 0 && <li>No hay jugadores.</li>}
            </ul>
          </div>
        )}
        {pestana === 'tarjetas' && <p>Por definir.</p>}
        {pestana === 'sanciones' && <p>Por definir.</p>}
        </div>
      </section>
    </div>
  )
}
