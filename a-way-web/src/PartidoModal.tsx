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
  tarjetas: { jugadorId: string; amarillas: number | null; rojas: number | null; observacion: string | null }[]
  sancionesDisponibles: { id: string; descripcion: string; costo: number; estado: string }[]
  sanciones: { jugadorId: string; observacion: string | null; sancionIds: string[] }[]
}

type Tarjeta = {
  amarilla: string
  roja: string
  observacion: string
}

type SancionJugador = {
  sancionIds: string[]
  observacion: string
}

type Jugador = {
  id: string
  numero: number
  alias: string
  hasFoto: boolean
  rotacion: number
  suspendido: boolean
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
  const [tarjetas, setTarjetas] = useState<Record<string, Tarjeta>>({})
  const [sanciones, setSanciones] = useState<Record<string, SancionJugador>>({})
  const [sancionElegida, setSancionElegida] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [grabado, setGrabado] = useState(false)

  function golesDe(respuesta: Detalle) {
    const cargados: Record<string, string> = {}
    for (const gol of respuesta.goles) cargados[gol.jugadorId] = String(gol.cantidad)
    return cargados
  }

  function tarjetasDe(respuesta: Detalle) {
    const cargadas: Record<string, Tarjeta> = {}
    for (const tarjeta of respuesta.tarjetas) {
      cargadas[tarjeta.jugadorId] = {
        amarilla: tarjeta.amarillas == null ? '' : String(tarjeta.amarillas),
        roja: tarjeta.rojas == null ? '' : String(tarjeta.rojas),
        observacion: tarjeta.observacion ?? '',
      }
    }
    return cargadas
  }

  function sancionesDe(respuesta: Detalle) {
    const cargadas: Record<string, SancionJugador> = {}
    for (const sancion of respuesta.sanciones) {
      cargadas[sancion.jugadorId] = {
        sancionIds: sancion.sancionIds,
        observacion: sancion.observacion ?? '',
      }
    }
    return cargadas
  }

  useEffect(() => {
    let activo = true
    api<Detalle>(`/api/resultados/partido/${partidoId}/detalle`)
      .then((respuesta) => {
        if (!activo) return
        setDetalle(respuesta)
        setAsignados(respuesta.asignados)
        setGoles(golesDe(respuesta))
        setTarjetas(tarjetasDe(respuesta))
        setSanciones(sancionesDe(respuesta))
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
          tarjetas: Object.entries(tarjetas)
            .filter(([, tarjeta]) => tarjeta.amarilla !== '' || tarjeta.roja !== '' || tarjeta.observacion.trim() !== '')
            .map(([jugadorId, tarjeta]) => ({
              jugadorId,
              amarillas: tarjeta.amarilla === '' ? null : Number(tarjeta.amarilla),
              rojas: tarjeta.roja === '' ? null : Number(tarjeta.roja),
              observacion: tarjeta.observacion.trim() || null,
            })),
          sanciones: Object.entries(sanciones)
            .filter(([, sancion]) => sancion.sancionIds.length > 0 || sancion.observacion.trim() !== '')
            .map(([jugadorId, sancion]) => ({
              jugadorId,
              observacion: sancion.observacion.trim() || null,
              sancionIds: sancion.sancionIds,
            })),
        }),
      })
      setDetalle(respuesta)
      setAsignados(respuesta.asignados)
      setGoles(golesDe(respuesta))
      setTarjetas(tarjetasDe(respuesta))
      setSanciones(sancionesDe(respuesta))
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
                  {jugador.suspendido && <span>Suspendido</span>}
                  <input
                    className="marcador"
                    inputMode="numeric"
                    aria-label="Goles"
                    value={goles[jugador.id] ?? ''}
                    disabled={!detalle?.editable || jugador.suspendido}
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
        {pestana === 'tarjetas' && (
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
            <div className="lista-tarjetas-encabezado">
              <span />
              <span />
              <span />
              <span className="icono-tarjeta amarilla" role="img" aria-label="Tarjeta amarilla" />
              <span className="icono-tarjeta roja" role="img" aria-label="Tarjeta roja" />
              <span>Observación</span>
            </div>
            <ul className="lista-detalle tarjetas">
              {jugadores.map((jugador) => {
                const tarjeta = tarjetas[jugador.id] ?? { amarilla: '', roja: '', observacion: '' }
                function cambiar(parcial: Partial<Tarjeta>) {
                  setGrabado(false)
                  setTarjetas((anterior) => {
                    const actual = anterior[jugador.id] ?? { amarilla: '', roja: '', observacion: '' }
                    return { ...anterior, [jugador.id]: { ...actual, ...parcial } }
                  })
                }
                return (
                  <li key={jugador.id}>
                    <FotoJugador jugador={jugador} />
                    <span>{jugador.numero}</span>
                    <span>{jugador.alias}</span>
                    {jugador.suspendido && <span>Suspendido</span>}
                    <input
                      className="marcador"
                      inputMode="numeric"
                      aria-label="Tarjeta amarilla"
                      value={tarjeta.amarilla}
                      disabled={!detalle?.editable || jugador.suspendido}
                      onChange={(event) => {
                        const valor = event.target.value
                        if (valor !== '' && valor !== '1' && valor !== '2') return
                        cambiar({ amarilla: valor })
                      }}
                    />
                    <input
                      className="marcador"
                      inputMode="numeric"
                      aria-label="Tarjeta roja"
                      value={tarjeta.roja}
                      disabled={!detalle?.editable || jugador.suspendido}
                      onChange={(event) => {
                        const valor = event.target.value
                        if (valor !== '' && valor !== '1') return
                        cambiar({ roja: valor })
                      }}
                    />
                    <input
                      aria-label="Observación"
                      maxLength={300}
                      value={tarjeta.observacion}
                      disabled={!detalle?.editable || jugador.suspendido}
                      onChange={(event) => cambiar({ observacion: event.target.value })}
                    />
                  </li>
                )
              })}
              {jugadores.length === 0 && <li className="vacio">No hay jugadores.</li>}
            </ul>
          </div>
        )}
        {pestana === 'sanciones' && (
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
            <ul className="lista-detalle sanciones">
          {jugadores.map((jugador) => {
            const sancion = sanciones[jugador.id] ?? { sancionIds: [], observacion: '' }
            const disponibles = (detalle?.sancionesDisponibles ?? []).filter((opcion) =>
              opcion.estado === 'ACTIVO' && !sancion.sancionIds.includes(opcion.id))
            const nombreDe = (id: string) => detalle?.sancionesDisponibles.find((opcion) => opcion.id === id)?.descripcion ?? 'Sanción'
            return (
              <li key={jugador.id}>
                <FotoJugador jugador={jugador} />
                <span className="numero">{jugador.numero}</span>
                <span>{jugador.alias}</span>
                {jugador.suspendido && <span>Suspendido</span>}
                <div className="sancion-jugador">
                  {detalle?.editable && !jugador.suspendido && (
                    <div className="agregar-sancion">
                      <select
                        aria-label={`Sanción de ${jugador.alias}`}
                        value={sancionElegida[jugador.id] ?? ''}
                        onChange={(event) => setSancionElegida((anterior) => ({ ...anterior, [jugador.id]: event.target.value }))}
                      >
                        <option value="">Sanción</option>
                        {disponibles.map((opcion) => <option key={opcion.id} value={opcion.id}>{opcion.descripcion}</option>)}
                      </select>
                      <button
                        type="button"
                        className="secundario"
                        disabled={!sancionElegida[jugador.id]}
                        onClick={() => {
                          const elegida = sancionElegida[jugador.id]
                          if (!elegida) return
                          setSanciones((anterior) => {
                            const actual = anterior[jugador.id] ?? { sancionIds: [], observacion: '' }
                            return { ...anterior, [jugador.id]: { ...actual, sancionIds: [...actual.sancionIds, elegida] } }
                          })
                          setSancionElegida((anterior) => ({ ...anterior, [jugador.id]: '' }))
                          setGrabado(false)
                        }}
                      >
                        Agregar
                      </button>
                    </div>
                  )}
                  <ul className="sanciones-elegidas">
                    {sancion.sancionIds.map((id) => (
                      <li key={id}>
                        <span>{nombreDe(id)}</span>
                        {detalle?.editable && !jugador.suspendido && (
                          <button
                            type="button"
                            className="secundario"
                            onClick={() => {
                              setSanciones((anterior) => {
                                const actual = anterior[jugador.id] ?? { sancionIds: [], observacion: '' }
                                return { ...anterior, [jugador.id]: { ...actual, sancionIds: actual.sancionIds.filter((item) => item !== id) } }
                              })
                              setGrabado(false)
                            }}
                          >
                            Quitar
                          </button>
                        )}
                      </li>
                    ))}
                  </ul>
                  <input
                    aria-label={`Observación de ${jugador.alias}`}
                    placeholder="Observación"
                    maxLength={300}
                    value={sancion.observacion}
                    disabled={!detalle?.editable || jugador.suspendido}
                    onChange={(event) => {
                      const valor = event.target.value
                      setSanciones((anterior) => {
                        const actual = anterior[jugador.id] ?? { sancionIds: [], observacion: '' }
                        return { ...anterior, [jugador.id]: { ...actual, observacion: valor } }
                      })
                      setGrabado(false)
                    }}
                  />
                </div>
              </li>
            )
          })}
          {jugadores.length === 0 && <li className="vacio">No hay jugadores.</li>}
        </ul>
          </div>
        )}
        </div>
      </section>
    </div>
  )
}
