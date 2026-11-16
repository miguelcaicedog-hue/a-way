import { useEffect, useState, type FormEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Cancha = {
  id: string
  leagueId: string
  descripcion: string
  estado: string
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

function etiqueta(estado: string) {
  return estado === 'INACTIVO' ? 'Inactivo' : 'Activo'
}

export function CanchasScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [rows, setRows] = useState<Cancha[]>([])
  const [filtroDescripcion, setFiltroDescripcion] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [descripcion, setDescripcion] = useState('')
  const [estado, setEstado] = useState('ACTIVO')
  const [editing, setEditing] = useState<Cancha | null>(null)
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  function url(texto: string, estadoFiltro: string) {
    const params = new URLSearchParams({ leagueId })
    if (texto.trim()) params.set('descripcion', texto.trim())
    if (estadoFiltro) params.set('estado', estadoFiltro)
    return `/api/canchas?${params.toString()}`
  }

  function cargar(texto: string, estadoFiltro: string) {
    return api<Cancha[]>(url(texto, estadoFiltro)).then(setRows)
  }

  useEffect(() => {
    if (!leagueId) {
      setRows([])
      return
    }
    cargar(filtroDescripcion, filtroEstado).catch((reason: Error) => setError(reason.message))
  }, [leagueId])

  function consultar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    cargar(filtroDescripcion, filtroEstado).catch((reason: Error) => setError(reason.message))
  }

  function reset() {
    setEditing(null)
    setDescripcion('')
    setEstado('ACTIVO')
  }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = { leagueId, descripcion: descripcion.trim(), estado }
    try {
      if (editing) {
        await api(`/api/canchas/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/canchas', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      await cargar(filtroDescripcion, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar la cancha')
    }
  }

  async function eliminar(row: Cancha) {
    setError(null)
    try {
      await api(`/api/canchas/${row.id}`, { method: 'DELETE' })
      if (editing?.id === row.id) reset()
      await cargar(filtroDescripcion, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar la cancha')
    }
  }

  return (
    <section className="catalog">
      {!leagueId && <p>Selecciona una liga.</p>}
      {leagueId && (
        <form className="filtros" onSubmit={consultar}>
          <label>
            Descripción
            <input value={filtroDescripcion} onChange={(event) => setFiltroDescripcion(event.target.value)} />
          </label>
          <label>
            Estado
            <select value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
              <option value="">Todos</option>
            </select>
          </label>
          <button type="submit">Consultar</button>
        </form>
      )}
      <table>
        <thead>
          <tr>
            <th>Descripción</th>
            <th>Estado</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.descripcion}</td>
              <td>{etiqueta(row.estado)}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => { setEditing(row); setDescripcion(row.descripcion); setEstado(row.estado) }}>Editar</button>
                  <button type="button" className="ghost" onClick={() => eliminar(row)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={writable ? 3 : 2}>No hay canchas.</td></tr>}
        </tbody>
      </table>
      {writable && leagueId && (
        <form onSubmit={guardar}>
          <h2>{editing ? 'Editar cancha' : 'Nueva cancha'}</h2>
          <label>
            Descripción
            <input value={descripcion} maxLength={160} required onChange={(event) => setDescripcion(event.target.value)} />
          </label>
          <label>
            Estado
            <select value={estado} onChange={(event) => setEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <div className="actions">
            <button type="submit">Guardar</button>
            {editing && <button type="button" className="ghost" onClick={reset}>Cancelar</button>}
          </div>
          {error && <p className="down">{error}</p>}
        </form>
      )}
    </section>
  )
}
