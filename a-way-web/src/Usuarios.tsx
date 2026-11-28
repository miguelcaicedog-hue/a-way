import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { api } from './api'
import { useFocoEdicion } from './foco'

type Profile = {
  master: boolean
}

type Usuario = {
  id: string
  username: string
  displayName: string
  master: boolean
  estado: string
  role: string | null
  hasFoto: boolean
}

function etiqueta(estado: string) {
  if (estado === 'INACTIVO') return 'Inactivo'
  if (estado === 'BLOQUEADO') return 'Bloqueado'
  return 'Activo'
}

function etiquetaRol(rol: string | null) {
  if (rol === 'ADMINISTRATIVO') return 'Administrativo'
  if (rol === 'ASISTENTE') return 'Asistente'
  return ''
}

function FotoUsuario({ id, hasFoto, stamp }: { id?: string; hasFoto: boolean; stamp: number }) {
  if (hasFoto && id) {
    return <img className="arbitro-foto" alt="" src={`/api/usuarios/${id}/foto?v=${stamp}`} />
  }
  return <span className="arbitro-foto vacio" aria-label="Sin foto" />
}

export function UsuariosScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [rows, setRows] = useState<Usuario[]>([])
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [estado, setEstado] = useState('ACTIVO')
  const [role, setRole] = useState('ASISTENTE')
  const [foto, setFoto] = useState<string | null>(null)
  const [fotoTipo, setFotoTipo] = useState<string | null>(null)
  const [quitarFoto, setQuitarFoto] = useState(false)
  const [fotoStamp, setFotoStamp] = useState(0)
  const [editing, setEditing] = useState<Usuario | null>(null)
  const formEdicion = useFocoEdicion(editing?.id ?? null)
  const [error, setError] = useState<string | null>(null)

  function url(estadoFiltro: string) {
    const params = new URLSearchParams({ leagueId })
    if (estadoFiltro) params.set('estado', estadoFiltro)
    return `/api/usuarios?${params.toString()}`
  }

  function cargar(estadoFiltro: string) {
    return api<Usuario[]>(url(estadoFiltro)).then(setRows)
  }

  useEffect(() => {
    if (!leagueId || !profile.master) {
      setRows([])
      return
    }
    cargar(filtroEstado).catch((reason: Error) => setError(reason.message))
  }, [leagueId])

  function consultar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    cargar(filtroEstado).catch((reason: Error) => setError(reason.message))
  }

  function reset() {
    setEditing(null)
    setUsername('')
    setDisplayName('')
    setPassword('')
    setEstado('ACTIVO')
    setRole('ASISTENTE')
    setFoto(null)
    setFotoTipo(null)
    setQuitarFoto(false)
  }

  function editar(row: Usuario) {
    setEditing(row)
    setUsername(row.username)
    setDisplayName(row.displayName)
    setPassword('')
    setEstado(row.master ? 'ACTIVO' : row.estado)
    setRole(row.role === 'ADMINISTRATIVO' ? 'ADMINISTRATIVO' : 'ASISTENTE')
    setFoto(null)
    setFotoTipo(null)
    setQuitarFoto(false)
    setError(null)
  }

  function onFoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    if (file.size > 1_500_000) {
      setError('La foto debe pesar menos de 1.5 MB')
      event.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : null
      const declared = (file.type || dataUrl?.slice(5, dataUrl.indexOf(';')) || '').toLowerCase()
      const type = declared === 'image/jpg' || declared === 'image/pjpeg' ? 'image/jpeg' : declared
      setFoto(dataUrl)
      setFotoTipo(type)
      setQuitarFoto(false)
      setError(null)
    }
    reader.readAsDataURL(file)
  }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = {
      leagueId,
      username: username.trim(),
      displayName: displayName.trim(),
      password: editing?.master ? null : (password.trim() || null),
      estado,
      role: editing?.master ? null : role,
      fotoBase64: foto,
      fotoContentType: fotoTipo,
      clearFoto: quitarFoto,
    }
    try {
      if (editing) {
        await api(`/api/usuarios/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/usuarios', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      setFotoStamp((value) => value + 1)
      await cargar(filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar el usuario')
    }
  }

  if (!profile.master) {
    return <p>Solo el master administra usuarios.</p>
  }

  return (
    <section className="catalog">
      {!leagueId && <p>Selecciona una liga.</p>}
      {error && <p className="down">{error}</p>}
      {leagueId && (
        <form className="filtros" onSubmit={consultar}>
          <label>
            Estado
            <select value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
              <option value="BLOQUEADO">Bloqueado</option>
              <option value="">Todos</option>
            </select>
          </label>
          <button type="submit">Consultar</button>
        </form>
      )}
      <table>
        <thead>
          <tr>
            <th>Foto</th>
            <th>Usuario</th>
            <th>Nombre</th>
            <th>Rol</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td><FotoUsuario id={row.id} hasFoto={row.hasFoto} stamp={fotoStamp} /></td>
              <td>{row.username}</td>
              <td>{row.displayName}</td>
              <td>{row.master ? 'Master' : etiquetaRol(row.role)}</td>
              <td>{etiqueta(row.estado)}</td>
              <td className="actions">
                <button type="button" onClick={() => editar(row)}>Editar</button>
              </td>
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={6}>No hay usuarios.</td></tr>}
        </tbody>
      </table>
      {leagueId && (
        <form ref={formEdicion} onSubmit={guardar}>
          <h2>{editing ? 'Editar usuario' : 'Nuevo usuario'}</h2>
          <label>
            Usuario
            <input value={username} maxLength={80} required disabled={editing != null} onChange={(event) => setUsername(event.target.value)} />
          </label>
          <label>
            Nombre
            <input value={displayName} maxLength={160} required onChange={(event) => setDisplayName(event.target.value)} />
          </label>
          {editing?.master !== true && (
            <label>
              Clave
              <input type="password" value={password} autoComplete="new-password" required={editing == null} onChange={(event) => setPassword(event.target.value)} />
            </label>
          )}
          <label>
            Estado
            <select value={estado} disabled={editing?.master === true} onChange={(event) => setEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
              <option value="BLOQUEADO">Bloqueado</option>
            </select>
          </label>
          {editing?.master !== true && (
            <label>
              Rol
              <select value={role} required onChange={(event) => setRole(event.target.value)}>
                <option value="ADMINISTRATIVO">Administrativo</option>
                <option value="ASISTENTE">Asistente</option>
              </select>
            </label>
          )}
          <label>
            Foto
            <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={onFoto} />
          </label>
          {editing?.hasFoto && (
            <label className="check">
              <input type="checkbox" checked={quitarFoto} onChange={(event) => setQuitarFoto(event.target.checked)} />
              Quitar foto
            </label>
          )}
          <button type="submit">Guardar</button>
          {editing && <button type="button" className="ghost" onClick={reset}>Cancelar</button>}
        </form>
      )}
    </section>
  )
}
