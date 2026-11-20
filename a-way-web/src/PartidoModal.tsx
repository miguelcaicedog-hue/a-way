import { useEffect, useState } from 'react'
import { api } from './api'
import { aDia } from './Fecha'

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
  golesLocal: number | null
  golesVisita: number | null
  jornadaNombre: string
  fecha: string | null
  hora: string | null
  canchaNombre: string | null
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
  habilitado: boolean
  motivoSuspension: string | null
}

function FotoArbitro({ id, hasFoto }: { id: string; hasFoto: boolean }) {
  if (hasFoto) {
    return <img className="arbitro-foto" alt="" src={`/api/arbitros/${id}/foto`} />
  }
  return <span className="arbitro-foto vacio" aria-label="Sin foto" />
}

function bordeFoto(jugador: Jugador) {
  if (jugador.suspendido) return 'borde-suspendido'
  if (!jugador.habilitado) return 'borde-inhabilitado'
  return 'borde-habilitado'
}

function jugadorBloqueado(jugador: Jugador) {
  return jugador.suspendido || !jugador.habilitado
}

function EstadoJugador({ jugador }: { jugador: Jugador }) {
  if (jugador.suspendido) {
    const motivo = jugador.motivoSuspension ? ` · ${jugador.motivoSuspension}` : ''
    return <span className="estado-jugador">Suspendido{motivo}</span>
  }
  if (!jugador.habilitado) return <span className="estado-jugador">No habilitado</span>
  return null
}

function FotoJugador({ jugador }: { jugador: Jugador }) {
  const borde = bordeFoto(jugador)
  if (jugador.hasFoto) {
    return (
      <img
        className={`player-photo ${borde}`}
        alt=""
        src={`/api/players/${jugador.id}/photo`}
        style={{ transform: `rotate(${jugador.rotacion}deg)` }}
      />
    )
  }
  return <span className={`player-photo vacio ${borde}`} aria-label="Sin foto" />
}

function firmaArbitros(lista: Asignado[]) {
  return [...lista]
    .map((item) => ({ arbitroId: item.arbitroId, pito: item.pito }))
    .sort((izquierda, derecha) => izquierda.arbitroId.localeCompare(derecha.arbitroId))
}

function firmaGoles(mapa: Record<string, string>) {
  return Object.entries(mapa)
    .filter(([, cantidad]) => cantidad !== '' && Number(cantidad) > 0)
    .map(([jugadorId, cantidad]) => ({ jugadorId, cantidad: Number(cantidad) }))
    .sort((izquierda, derecha) => izquierda.jugadorId.localeCompare(derecha.jugadorId))
}

function firmaTarjetas(mapa: Record<string, Tarjeta>) {
  return Object.entries(mapa)
    .filter(([, tarjeta]) => tarjeta.amarilla !== '' || tarjeta.roja !== '' || tarjeta.observacion.trim() !== '')
    .map(([jugadorId, tarjeta]) => ({
      jugadorId,
      amarillas: tarjeta.amarilla === '' ? null : Number(tarjeta.amarilla),
      rojas: tarjeta.roja === '' ? null : Number(tarjeta.roja),
      observacion: tarjeta.observacion.trim() || null,
    }))
    .sort((izquierda, derecha) => izquierda.jugadorId.localeCompare(derecha.jugadorId))
}

function firmaSanciones(mapa: Record<string, SancionJugador>) {
  return Object.entries(mapa)
    .filter(([, sancion]) => sancion.sancionIds.length > 0 || sancion.observacion.trim() !== '')
    .map(([jugadorId, sancion]) => ({
      jugadorId,
      observacion: sancion.observacion.trim() || null,
      sancionIds: [...sancion.sancionIds].sort(),
    }))
    .sort((izquierda, derecha) => izquierda.jugadorId.localeCompare(derecha.jugadorId))
}

function textoBalance(detalle: Detalle | null, equipoId: string, idsPlantilla: string[], goles: Record<string, string>) {
  if (!detalle || !equipoId) return ''
  const marcador = equipoId === detalle.equipoLocal?.id ? detalle.golesLocal : detalle.golesVisita
  const asignados = idsPlantilla.reduce((suma, id) => suma + (goles[id] ? Number(goles[id]) : 0), 0)
  const delEquipo = marcador == null ? '—' : String(marcador)
  const diferencia = marcador == null ? '—' : String(marcador - asignados)
  return `Goles del equipo: ${delEquipo} · Asignados: ${asignados} · Diferencia: ${diferencia}`
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
  const [idsPlantilla, setIdsPlantilla] = useState<string[]>([])
  const [goles, setGoles] = useState<Record<string, string>>({})
  const [tarjetas, setTarjetas] = useState<Record<string, Tarjeta>>({})
  const [sanciones, setSanciones] = useState<Record<string, SancionJugador>>({})
  const [sancionElegida, setSancionElegida] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [grabado, setGrabado] = useState(false)
  const [confirmarCierre, setConfirmarCierre] = useState(false)

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
        if (respuesta.asignados.length > 0) setPestana('goleadores')
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
      setIdsPlantilla([])
      return
    }
    let activo = true
    api<Jugador[]>(`/api/resultados/partido/${partidoId}/jugadores?teamId=${equipoId}`)
      .then((lista) => {
        if (activo) setIdsPlantilla(lista.map((jugador) => jugador.id))
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [partidoId, equipoId])

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
    const siguientes = [...asignados, { arbitroId: arbitro.id, pito: true }]
    setAsignados(siguientes)
    void grabar(siguientes)
  }

  function quitar(arbitroId: string) {
    const siguientes = asignados.filter((item) => item.arbitroId !== arbitroId)
    setAsignados(siguientes)
    void grabar(siguientes)
  }

  function marcarPito(arbitroId: string, pito: boolean) {
    const siguientes = asignados.map((item) => (item.arbitroId === arbitroId ? { ...item, pito } : item))
    setAsignados(siguientes)
    void grabar(siguientes)
  }

  async function grabar(arbitros = asignados) {
    if (!detalle?.editable) return
    setOcupado(true)
    setError(null)
    setGrabado(false)
    try {
      const respuesta = await api<Detalle>(`/api/resultados/partido/${partidoId}/detalle`, {
        method: 'PUT',
        body: JSON.stringify({
          arbitros,
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
      setConfirmarCierre(false)
    } catch (caught) {
      setGrabado(false)
      setError(caught instanceof Error ? caught.message : 'No se pudo grabar el partido')
    } finally {
      setOcupado(false)
    }
  }

  function hayCambios() {
    if (!detalle) return false
    const actual = JSON.stringify({
      arbitros: firmaArbitros(asignados),
      goles: firmaGoles(goles),
      tarjetas: firmaTarjetas(tarjetas),
      sanciones: firmaSanciones(sanciones),
    })
    const guardado = JSON.stringify({
      arbitros: firmaArbitros(detalle.asignados),
      goles: detalle.goles
        .filter((gol) => gol.cantidad > 0)
        .map((gol) => ({ jugadorId: gol.jugadorId, cantidad: gol.cantidad }))
        .sort((izquierda, derecha) => izquierda.jugadorId.localeCompare(derecha.jugadorId)),
      tarjetas: firmaTarjetas(tarjetasDe(detalle)),
      sanciones: firmaSanciones(sancionesDe(detalle)),
    })
    return actual !== guardado
  }

  function intentarCerrar() {
    if (hayCambios()) {
      setConfirmarCierre(true)
      return
    }
    onClose()
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
                <span className="dato-partido jornada">{detalle.jornadaNombre}</span>
                <span className="dato-partido">{[aDia(detalle.fecha), detalle.hora].filter(Boolean).join(' ')}</span>
                {detalle.canchaNombre && <span className="dato-partido jornada">{detalle.canchaNombre}</span>}
              </>
            ) : 'Partido'}
          </h2>
          <div className="actions">
            {grabado && <span className="aviso-grabado">Grabado</span>}
            <button type="button" className="ghost" onClick={intentarCerrar}>Cerrar</button>
            {detalle?.editable && <button type="button" disabled={ocupado} onClick={() => void grabar()}>Grabar</button>}
          </div>
        </div>
        {confirmarCierre && (
          <p className="aviso-sin-grabar">
            Hay cambios sin grabar.
            <button type="button" className="ghost" onClick={onClose}>Cerrar</button>
            <button type="button" onClick={() => setConfirmarCierre(false)}>Continuar</button>
          </p>
        )}
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
            <p className="balance-goles">{textoBalance(detalle, equipoId, idsPlantilla, goles)}</p>
            <ul className="lista-detalle goleadores">
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
                    disabled={!detalle?.editable || jugadorBloqueado(jugador)}
                    onChange={(event) => {
                      const valor = event.target.value
                      if (valor !== '' && !/^\d+$/.test(valor)) return
                      setGrabado(false)
                      setGoles({ ...goles, [jugador.id]: valor })
                    }}
                  />
                  <EstadoJugador jugador={jugador} />
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
                    <input
                      className="marcador"
                      inputMode="numeric"
                      aria-label="Tarjeta amarilla"
                      value={tarjeta.amarilla}
                      disabled={!detalle?.editable || jugadorBloqueado(jugador)}
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
                      disabled={!detalle?.editable || jugadorBloqueado(jugador)}
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
                      disabled={!detalle?.editable || jugadorBloqueado(jugador)}
                      onChange={(event) => cambiar({ observacion: event.target.value })}
                    />
                    <EstadoJugador jugador={jugador} />
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
                <EstadoJugador jugador={jugador} />
                <div className="sancion-jugador">
                  {detalle?.editable && !jugadorBloqueado(jugador) && (
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
                        {detalle?.editable && !jugadorBloqueado(jugador) && (
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
                    disabled={!detalle?.editable || jugadorBloqueado(jugador)}
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
