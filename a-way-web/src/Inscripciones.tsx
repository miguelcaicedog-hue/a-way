import { useEffect, useState, type ChangeEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Archivo = {
  id: string
  nombreArchivo: string
  fechaCreacion: string
}

type Equipo = {
  id: string
  numeroFila: number
  categoria: string
  nombreEquipo: string
  nombreRepresentante: string | null
  numeroTelefono: string | null
  seleccionado: boolean
  resultado: string | null
  detalle: string | null
}

type Detalle = {
  id: string
  nombreArchivo: string
  equipos: Equipo[]
}

type Jugador = {
  id: string
  cedula: string | null
  nombre: string | null
  alias: string | null
  numeroCamisa: string | null
  cargado: boolean
  fotoCargada: boolean
  motivo: string | null
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((membership) => membership.role === 'ADMINISTRATIVO')
}

function aliasFromName(fullName: string) {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 2) {
    return parts.join(' ')
  }
  if (parts.length < 4) {
    return ''
  }
  return `${parts[0]} ${parts[2]}`
}

export function InscripcionScreen({ profile, leagueId, editionId }: { profile: Profile; leagueId: string; editionId: string }) {
  const [archivos, setArchivos] = useState<Archivo[]>([])
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const [abierto, setAbierto] = useState<string | null>(null)
  const [jugadores, setJugadores] = useState<Jugador[]>([])
  const [error, setError] = useState<string | null>(null)
  const [procesando, setProcesando] = useState(false)
  const [progreso, setProgreso] = useState<{ hecho: number; total: number; nombre: string } | null>(null)
  const [revision, setRevision] = useState<Equipo | null>(null)
  const [comprobando, setComprobando] = useState(false)
  const [avisoFotos, setAvisoFotos] = useState<string | null>(null)
  const [editor, setEditor] = useState<Jugador | null>(null)
  const [formJugador, setFormJugador] = useState({ cedula: '', nombre: '', alias: '', numeroCamisa: '' })
  const [fotoJugador, setFotoJugador] = useState<File | null>(null)
  const [guardando, setGuardando] = useState(false)
  const writable = canWrite(profile)

  async function abrir(id: string) {
    const body = await api<Detalle>(`/api/inscripciones/${id}`)
    setDetalle(body)
    setAbierto(null)
    setJugadores([])
  }

  useEffect(() => {
    if (!leagueId || !editionId) {
      setArchivos([])
      setDetalle(null)
      return
    }
    api<Archivo[]>(`/api/inscripciones?leagueId=${leagueId}&editionId=${editionId}`)
      .then(async (items) => {
        setArchivos(items)
        if (items[0]) {
          await abrir(items[0].id)
        } else {
          setDetalle(null)
        }
      })
      .catch((reason: Error) => setError(reason.message))
  }, [leagueId, editionId])

  async function onFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !leagueId || !editionId) {
      return
    }
    setError(null)
    const body = new FormData()
    body.append('archivo', file)
    try {
      const response = await fetch(`/api/inscripciones?leagueId=${leagueId}&editionId=${editionId}`, {
        method: 'POST',
        credentials: 'include',
        body,
      })
      const payload = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(typeof payload.error === 'string' ? payload.error : 'No se pudo cargar el archivo')
      }
      setDetalle(payload as Detalle)
      setArchivos(await api<Archivo[]>(`/api/inscripciones?leagueId=${leagueId}&editionId=${editionId}`))
      setAbierto(null)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cargar el archivo')
    }
  }

  async function marcar(equipo: Equipo, seleccionado: boolean) {
    setError(null)
    try {
      const saved = await api<Equipo>(`/api/inscripciones/equipos/${equipo.id}/seleccion`, {
        method: 'PUT',
        body: JSON.stringify({ seleccionado }),
      })
      setDetalle((current) => current && ({
        ...current,
        equipos: current.equipos.map((item) => item.id === saved.id ? saved : item),
      }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo actualizar la selección')
    }
  }

  async function verJugadores(equipo: Equipo) {
    if (abierto === equipo.id) {
      setAbierto(null)
      return
    }
    setAbierto(equipo.id)
    try {
      setJugadores(await api<Jugador[]>(`/api/inscripciones/equipos/${equipo.id}/jugadores`))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudieron leer los jugadores')
    }
  }

  function abrirEditor(jugador: Jugador) {
    setEditor(jugador)
    setFotoJugador(null)
    setFormJugador({
      cedula: jugador.cedula ?? '',
      nombre: jugador.nombre ?? '',
      alias: jugador.alias || aliasFromName(jugador.nombre ?? ''),
      numeroCamisa: jugador.numeroCamisa ?? '',
    })
  }

  function cambiarNombre(nombre: string) {
    setFormJugador((current) => {
      const anterior = aliasFromName(current.nombre)
      const sugerido = aliasFromName(nombre)
      const alias = !current.alias || current.alias === anterior ? sugerido : current.alias
      return { ...current, nombre, alias }
    })
  }

  async function guardarJugador() {
    if (!editor || !detalle) {
      return
    }
    setError(null)
    setGuardando(true)
    const body = new FormData()
    body.append('cedula', formJugador.cedula.trim())
    body.append('nombre', formJugador.nombre.trim())
    body.append('alias', formJugador.alias.trim())
    body.append('numeroCamisa', formJugador.numeroCamisa.trim())
    if (fotoJugador) {
      body.append('foto', fotoJugador)
    }
    try {
      const response = await fetch(`/api/inscripciones/jugadores/${editor.id}`, {
        method: 'PUT',
        credentials: 'include',
        body,
      })
      const payload = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(typeof payload.error === 'string' ? payload.error : 'No se pudo guardar el jugador')
      }
      const saved = payload as Jugador
      setJugadores((current) => current.map((item) => item.id === saved.id ? saved : item))
      setDetalle(await api<Detalle>(`/api/inscripciones/${detalle.id}`))
      setEditor(null)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar el jugador')
    } finally {
      setGuardando(false)
    }
  }

  async function procesar(forzar = false) {
    if (!detalle) {
      return
    }
    const pendientes = detalle.equipos.filter((equipo) => equipo.seleccionado && equipo.resultado !== 'OK')
    if (pendientes.length === 0) {
      setError('No hay equipos pendientes para procesar')
      return
    }
    setError(null)
    setAvisoFotos(null)
    if (!forzar) {
      setComprobando(true)
      try {
        const acceso = await api<{ acceso: boolean; mensaje: string | null }>(`/api/inscripciones/${detalle.id}/acceso-fotos`, {
          method: 'POST',
          body: JSON.stringify({ equipoIds: pendientes.map((equipo) => equipo.id) }),
        })
        if (!acceso.acceso) {
          setAvisoFotos(acceso.mensaje ?? 'No hay acceso a las fotos de Google Drive.')
          return
        }
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : 'No se pudo comprobar el acceso a las fotos')
        return
      } finally {
        setComprobando(false)
      }
    }
    setProcesando(true)
    setAbierto(null)
    setJugadores([])
    setProgreso({ hecho: 0, total: pendientes.length, nombre: pendientes[0].nombreEquipo })
    try {
      for (let index = 0; index < pendientes.length; index += 1) {
        const equipo = pendientes[index]
        setProgreso({ hecho: index, total: pendientes.length, nombre: equipo.nombreEquipo })
        const saved = await api<Equipo>(`/api/inscripciones/equipos/${equipo.id}/procesar`, { method: 'POST' })
        setDetalle((current) => current && ({
          ...current,
          equipos: current.equipos.map((item) => item.id === saved.id ? saved : item),
        }))
        setProgreso({ hecho: index + 1, total: pendientes.length, nombre: equipo.nombreEquipo })
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo procesar')
    } finally {
      setProcesando(false)
      setProgreso(null)
    }
  }

  useEffect(() => {
    if (!revision && !avisoFotos && !editor) {
      return
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setRevision(null)
        setAvisoFotos(null)
        setEditor(null)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [revision, avisoFotos, editor])

  if (!leagueId || !editionId) {
    return <p>Selecciona una liga y una edición.</p>
  }

  const equipoAbierto = detalle?.equipos.find((equipo) => equipo.id === abierto)

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
      {writable && (
        <label>
          Archivo de inscripciones
          <input type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={onFile} />
        </label>
      )}
      {archivos.length > 0 && (
        <label>
          Archivo cargado
          <select value={detalle?.id ?? ''} onChange={(event) => abrir(event.target.value)}>
            {archivos.map((archivo) => (
              <option key={archivo.id} value={archivo.id}>{archivo.nombreArchivo}</option>
            ))}
          </select>
        </label>
      )}
      {detalle && (
        <>
          <table>
            <thead>
              <tr>
                {writable && <th></th>}
                <th className="numero">Nº</th>
                <th>Categoría</th>
                <th>Equipo</th>
                <th>Representante</th>
                <th>Teléfono</th>
                <th>Resultado</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {detalle.equipos.map((equipo, index) => (
                <tr key={equipo.id}>
                  {writable && (
                    <td>
                      <input
                        type="checkbox"
                        checked={equipo.seleccionado}
                        disabled={procesando || equipo.resultado === 'OK'}
                        onChange={(event) => marcar(equipo, event.target.checked)}
                      />
                    </td>
                  )}
                  <td className="numero">{index + 1}</td>
                  <td>{equipo.categoria}</td>
                  <td>
                    <button type="button" className="ghost" onClick={() => verJugadores(equipo)}>{equipo.nombreEquipo}</button>
                  </td>
                  <td>{equipo.nombreRepresentante}</td>
                  <td>{equipo.numeroTelefono}</td>
                  <td>{equipo.resultado}</td>
                  <td>
                    {equipo.detalle && (
                      <button type="button" className="icono" aria-label={`Ver detalle de ${equipo.nombreEquipo}`} onClick={() => setRevision(equipo)}>
                        <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                          <circle cx="8" cy="8" r="6.25" fill="none" stroke="currentColor" strokeWidth="1.4" />
                          <path d="M8 7.1v4.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                          <circle cx="8" cy="4.6" r="0.8" fill="currentColor" />
                        </svg>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {comprobando && <p>Comprobando el acceso a las fotos…</p>}
          {progreso && (
            <div className="progreso">
              <p>Procesando {Math.min(progreso.hecho + 1, progreso.total)} de {progreso.total}: {progreso.nombre}</p>
              <div className="progreso-pista" role="progressbar" aria-valuemin={0} aria-valuemax={progreso.total} aria-valuenow={progreso.hecho} aria-label="Avance del proceso">
                <div className="progreso-valor" style={{ width: `${progreso.total === 0 ? 0 : (progreso.hecho / progreso.total) * 100}%` }} />
              </div>
            </div>
          )}
          {writable && (
            <button type="button" onClick={() => procesar()} disabled={procesando || comprobando}>
              {procesando ? 'Procesando…' : comprobando ? 'Comprobando…' : 'Procesar'}
            </button>
          )}
          {equipoAbierto && (
            <div className="inscripcion-jugadores">
              <h2>{equipoAbierto.nombreEquipo}</h2>
              {!equipoAbierto.resultado && <p>Este equipo todavía no tiene jugadores cargados. Puedes corregir los datos antes de procesar.</p>}
              {jugadores.length > 0 && (
                <table>
                  <thead>
                    <tr>
                      <th className="numero">Nº</th>
                      <th>Cédula</th>
                      <th>Nombre</th>
                      <th>Alias</th>
                      <th>Nº de camisa</th>
                      <th>Foto</th>
                      <th>Revisar</th>
                      {writable && <th></th>}
                    </tr>
                  </thead>
                  <tbody>
                    {jugadores.map((jugador, index) => (
                      <tr key={jugador.id}>
                        <td className="numero">{index + 1}</td>
                        <td>{jugador.cedula}</td>
                        <td>{jugador.nombre}</td>
                        <td>{jugador.alias}</td>
                        <td>{jugador.numeroCamisa}</td>
                        <td>{jugador.fotoCargada ? 'Guardada' : jugador.cargado ? 'Sin foto' : 'No cargado'}</td>
                        <td className="detalle">{jugador.motivo}</td>
                        {writable && (
                          <td>
                            <button type="button" className="ghost" onClick={() => abrirEditor(jugador)}>Editar</button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
          {avisoFotos && (
            <div className="modal-fondo" onClick={() => setAvisoFotos(null)}>
              <div className="modal" role="dialog" aria-modal="true" aria-labelledby="aviso-fotos-titulo" onClick={(event) => event.stopPropagation()}>
                <h2 id="aviso-fotos-titulo">Fotos sin acceso</h2>
                <p className="detalle">{avisoFotos}</p>
                <div className="actions">
                  <button type="button" onClick={() => procesar(true)}>Procesar de todas formas</button>
                  <button type="button" className="ghost" onClick={() => setAvisoFotos(null)}>Cancelar</button>
                </div>
              </div>
            </div>
          )}
          {editor && (
            <div className="modal-fondo" onClick={() => setEditor(null)}>
              <div className="modal" role="dialog" aria-modal="true" aria-labelledby="editor-jugador-titulo" onClick={(event) => event.stopPropagation()}>
                <h2 id="editor-jugador-titulo">Editar jugador</h2>
                <form onSubmit={(event) => { event.preventDefault(); void guardarJugador() }}>
                  <label>Cédula<input value={formJugador.cedula} maxLength={30} onChange={(event) => setFormJugador({ ...formJugador, cedula: event.target.value })} required /></label>
                  <label>Nombre<input value={formJugador.nombre} maxLength={200} onChange={(event) => cambiarNombre(event.target.value)} required /></label>
                  <label>Alias<input value={formJugador.alias} maxLength={80} onChange={(event) => setFormJugador({ ...formJugador, alias: event.target.value })} /></label>
                  <label>Número de camisa<input value={formJugador.numeroCamisa} onChange={(event) => setFormJugador({ ...formJugador, numeroCamisa: event.target.value })} required /></label>
                  <label>
                    Foto
                    <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={(event) => setFotoJugador(event.target.files?.[0] ?? null)} />
                  </label>
                  <p>{editor.cargado ? 'Si eliges una foto, reemplaza la del plantel.' : 'Si eliges una foto, se aplicará al procesar el equipo.'}</p>
                  <div className="actions">
                    <button type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
                    <button type="button" className="ghost" onClick={() => setEditor(null)}>Cancelar</button>
                  </div>
                </form>
              </div>
            </div>
          )}
          {revision && (
            <div className="modal-fondo" onClick={() => setRevision(null)}>
              <div className="modal" role="dialog" aria-modal="true" aria-labelledby="detalle-titulo" onClick={(event) => event.stopPropagation()}>
                <h2 id="detalle-titulo">{revision.nombreEquipo}</h2>
                <p className="resultado">{revision.resultado}</p>
                <p className="detalle">{revision.detalle}</p>
                <button type="button" onClick={() => setRevision(null)}>Cerrar</button>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  )
}
