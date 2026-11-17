import { useEffect, useState } from 'react'
import { api } from './api'
import { aDia } from './Fecha'

type Equipo = { id: string; nombre: string }

type Cargo = {
  id: string
  jugadorId: string
  numero: number
  alias: string
  hasFoto: boolean
  rotacion: number
  jornada: string
  detalle: string
  costo: number
  marcado: boolean
}

type Cobro = {
  local: string
  visita: string
  equipoLocal: Equipo | null
  equipoVisita: Equipo | null
  jornadaNombre: string
  fecha: string | null
  hora: string | null
  canchaNombre: string | null
  editable: boolean
  equipoId: string
  equipoNombre: string
  arbitraje: number
  tarjetas: Cargo[]
  sanciones: Cargo[]
  observacion: string | null
  total: number
  valorCobroEfectivo: number
  valorCobroTransferencia: number
}

function dinero(valor: number) {
  return Number(valor ?? 0).toFixed(2)
}

function marcados(cargos: Cargo[]) {
  return new Set(cargos.filter((cargo) => cargo.marcado).map((cargo) => cargo.id))
}

function suma(cargos: Cargo[], ids: Set<string>) {
  return cargos.reduce((total, cargo) => total + (ids.has(cargo.id) ? Number(cargo.costo) : 0), 0)
}

function Foto({ cargo }: { cargo: Cargo }) {
  if (cargo.hasFoto) {
    return (
      <img
        className="player-photo"
        alt=""
        src={`/api/players/${cargo.jugadorId}/photo`}
        style={{ transform: `rotate(${cargo.rotacion}deg)` }}
      />
    )
  }
  return <span className="player-photo vacio" aria-label="Sin foto" />
}

export function CobroModal({ partidoId, onClose }: { partidoId: string; onClose: () => void }) {
  const [cobro, setCobro] = useState<Cobro | null>(null)
  const [equipoId, setEquipoId] = useState('')
  const [tarjetas, setTarjetas] = useState<Set<string>>(new Set())
  const [sanciones, setSanciones] = useState<Set<string>>(new Set())
  const [observacion, setObservacion] = useState('')
  const [efectivo, setEfectivo] = useState('0.00')
  const [transferencia, setTransferencia] = useState('0.00')
  const [verTarjetas, setVerTarjetas] = useState(false)
  const [verSanciones, setVerSanciones] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [grabado, setGrabado] = useState(false)

  function aplicar(respuesta: Cobro) {
    setCobro(respuesta)
    setEquipoId(respuesta.equipoId)
    setTarjetas(marcados(respuesta.tarjetas))
    setSanciones(marcados(respuesta.sanciones))
    setObservacion(respuesta.observacion ?? '')
    setEfectivo(dinero(respuesta.valorCobroEfectivo))
    setTransferencia(dinero(respuesta.valorCobroTransferencia))
  }

  function cargar(equipo?: string) {
    const params = new URLSearchParams()
    if (equipo) params.set('teamId', equipo)
    const consulta = params.toString()
    return api<Cobro>(`/api/resultados/partido/${partidoId}/cobro${consulta ? `?${consulta}` : ''}`).then(aplicar)
  }

  useEffect(() => {
    cargar().catch((reason: Error) => setError(reason.message))
  }, [partidoId])

  function cambiarEquipo(id: string) {
    setEquipoId(id)
    setGrabado(false)
    setVerTarjetas(false)
    setVerSanciones(false)
    cargar(id).catch((reason: Error) => setError(reason.message))
  }

  function alternar(ids: Set<string>, id: string, asignar: (valor: Set<string>) => void) {
    const siguiente = new Set(ids)
    if (siguiente.has(id)) siguiente.delete(id)
    else siguiente.add(id)
    asignar(siguiente)
    setGrabado(false)
  }

  async function grabar() {
    if (!cobro?.editable || !equipoId) return
    setOcupado(true)
    setError(null)
    try {
      const respuesta = await api<Cobro>(`/api/resultados/partido/${partidoId}/cobro`, {
        method: 'PUT',
        body: JSON.stringify({
          equipoId,
          tarjetaIds: [...tarjetas],
          sancionIds: [...sanciones],
          observacion: observacion.trim() || null,
          valorCobroEfectivo: efectivo === '' ? 0 : Number(efectivo),
          valorCobroTransferencia: transferencia === '' ? 0 : Number(transferencia),
        }),
      })
      aplicar(respuesta)
      setGrabado(true)
    } catch (caught) {
      setGrabado(false)
      setError(caught instanceof Error ? caught.message : 'No se pudo grabar el cobro')
    } finally {
      setOcupado(false)
    }
  }

  const equipos = [cobro?.equipoLocal, cobro?.equipoVisita].filter((equipo): equipo is Equipo => Boolean(equipo))
  const valorTarjetas = suma(cobro?.tarjetas ?? [], tarjetas)
  const valorSanciones = suma(cobro?.sanciones ?? [], sanciones)
  const total = Number(cobro?.arbitraje ?? 0) + valorTarjetas + valorSanciones
  const diferencia = (efectivo === '' ? 0 : Number(efectivo)) + (transferencia === '' ? 0 : Number(transferencia)) - total

  return (
    <div className="modal-fondo" role="presentation">
      <section className="modal partido" role="dialog" aria-modal="true" aria-label="Cobro del partido">
        <div className="partido-cabecera">
          <h2>
            {cobro ? (
              <>
                <span className="equipo-local">{cobro.local}</span>
                <span className="vs">VS</span>
                <span className="equipo-visita">{cobro.visita}</span>
                <span className="dato-partido jornada">{cobro.jornadaNombre}</span>
                <span className="dato-partido">{[aDia(cobro.fecha), cobro.hora].filter(Boolean).join(' ')}</span>
                {cobro.canchaNombre && <span className="dato-partido jornada">{cobro.canchaNombre}</span>}
              </>
            ) : 'Partido'}
          </h2>
          <div className="actions">
            {grabado && <span className="aviso-grabado">Grabado</span>}
            <button type="button" className="ghost" onClick={onClose}>Cerrar</button>
            {cobro?.editable && <button type="button" disabled={ocupado} onClick={grabar}>Grabar</button>}
          </div>
        </div>
        {error && <p className="down">{error}</p>}
        <div className="partido-cuerpo">
          <form className="filtros" onSubmit={(event) => event.preventDefault()}>
            <label>
              Equipo
              <select value={equipoId} onChange={(event) => cambiarEquipo(event.target.value)}>
                {equipos.map((equipo) => (
                  <option key={equipo.id} value={equipo.id}>{equipo.nombre}</option>
                ))}
              </select>
            </label>
          </form>
          <section className="cobro-bloque">
            <h3>Pago de arbitraje</h3>
            <p className="cobro-linea">
              <span>{cobro?.equipoNombre}</span>
              <span>Valor a cobrar por arbitraje</span>
              <strong>{dinero(cobro?.arbitraje ?? 0)}</strong>
            </p>
          </section>
          <section className="cobro-bloque">
            <h3>Pago de tarjetas</h3>
            <p className="cobro-linea">
              <span>{cobro?.equipoNombre}</span>
              <span>Valor a cobrar por tarjetas</span>
              <strong>{dinero(valorTarjetas)}</strong>
              <button type="button" className="lupa" aria-label="Ver tarjetas por cobrar" onClick={() => setVerTarjetas((valor) => !valor)}>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M10.5 3a7.5 7.5 0 0 1 5.9 12.1l4 4a1 1 0 0 1-1.4 1.4l-4-4A7.5 7.5 0 1 1 10.5 3zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11z" />
                </svg>
              </button>
            </p>
            {verTarjetas && (
              <ul className="lista-detalle cobro-cargos">
                {(cobro?.tarjetas ?? []).map((cargo) => (
                  <li key={cargo.id}>
                    <label className="check">
                      <input
                        type="checkbox"
                        checked={tarjetas.has(cargo.id)}
                        disabled={!cobro?.editable}
                        onChange={() => alternar(tarjetas, cargo.id, setTarjetas)}
                      />
                    </label>
                    <Foto cargo={cargo} />
                    <span className="numero">{cargo.numero}</span>
                    <span>{cargo.alias}</span>
                    <span>{cargo.jornada}</span>
                    <span>{cargo.detalle}</span>
                    <strong>{dinero(cargo.costo)}</strong>
                  </li>
                ))}
                {(cobro?.tarjetas.length ?? 0) === 0 && <li className="vacio">No hay tarjetas por cobrar.</li>}
              </ul>
            )}
          </section>
          <section className="cobro-bloque">
            <h3>Pago de sanciones</h3>
            <p className="cobro-linea">
              <span>{cobro?.equipoNombre}</span>
              <span>Valor a cobrar por sanciones</span>
              <strong>{dinero(valorSanciones)}</strong>
              <button type="button" className="lupa" aria-label="Ver sanciones por cobrar" onClick={() => setVerSanciones((valor) => !valor)}>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M10.5 3a7.5 7.5 0 0 1 5.9 12.1l4 4a1 1 0 0 1-1.4 1.4l-4-4A7.5 7.5 0 1 1 10.5 3zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11z" />
                </svg>
              </button>
            </p>
            {verSanciones && (
              <ul className="lista-detalle cobro-cargos">
                {(cobro?.sanciones ?? []).map((cargo) => (
                  <li key={cargo.id}>
                    <label className="check">
                      <input
                        type="checkbox"
                        checked={sanciones.has(cargo.id)}
                        disabled={!cobro?.editable}
                        onChange={() => alternar(sanciones, cargo.id, setSanciones)}
                      />
                    </label>
                    <Foto cargo={cargo} />
                    <span className="numero">{cargo.numero}</span>
                    <span>{cargo.alias}</span>
                    <span>{cargo.jornada}</span>
                    <span>{cargo.detalle}</span>
                    <strong>{dinero(cargo.costo)}</strong>
                  </li>
                ))}
                {(cobro?.sanciones.length ?? 0) === 0 && <li className="vacio">No hay sanciones por cobrar.</li>}
              </ul>
            )}
          </section>
          <section className="cobro-bloque">
            <h3>Valor a cobrar</h3>
            <div className="cobro-linea total">
              <span>Total</span>
              <strong>{dinero(total)}</strong>
              <label>
                Valor cobro efectivo
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={efectivo}
                  disabled={!cobro?.editable}
                  onChange={(event) => {
                    setEfectivo(event.target.value)
                    setGrabado(false)
                  }}
                />
              </label>
              <label>
                Valor cobro transferencia
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={transferencia}
                  disabled={!cobro?.editable}
                  onChange={(event) => {
                    setTransferencia(event.target.value)
                    setGrabado(false)
                  }}
                />
              </label>
              <span className={diferencia < 0 ? 'diferencia-cobro negativo' : 'diferencia-cobro positivo'}>
                Diferencia
                <strong>{dinero(diferencia)}</strong>
              </span>
            </div>
            <label>
              Observación
              <input
                maxLength={300}
                value={observacion}
                disabled={!cobro?.editable}
                onChange={(event) => {
                  setObservacion(event.target.value)
                  setGrabado(false)
                }}
              />
            </label>
          </section>
        </div>
      </section>
    </div>
  )
}
