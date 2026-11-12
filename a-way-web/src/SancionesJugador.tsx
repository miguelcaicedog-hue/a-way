import { useEffect, useState, type FormEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Sancion = {
  id: string
  leagueId: string
  descripcion: string
  costo: number
  estado: string
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

function etiqueta(estado: string) {
  return estado === 'ACTIVO' ? 'Activo' : 'Inactivo'
}

function dinero(costo: number) {
  return Number(costo).toFixed(2)
}

export function SancionesJugadorScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [rows, setRows] = useState<Sancion[]>([])
  const [filtroDescripcion, setFiltroDescripcion] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [descripcion, setDescripcion] = useState('')
  const [costo, setCosto] = useState('0')
  const [estado, setEstado] = useState('ACTIVO')
  const [editing, setEditing] = useState<Sancion | null>(null)
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  function url(texto: string, estadoFiltro: string) {
    const params = new URLSearchParams({ leagueId })
    if (texto.trim()) params.set('descripcion', texto.trim())
    if (estadoFiltro) params.set('estado', estadoFiltro)
    return `/api/sanciones-jugador?${params.toString()}`
  }

  function cargar(texto: string, estadoFiltro: string) {
    return api<Sancion[]>(url(texto, estadoFiltro)).then(setRows)
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
    setCosto('0')
    setEstado('ACTIVO')
  }

  function editar(row: Sancion) {
    setEditing(row)
    setDescripcion(row.descripcion)
    setCosto(dinero(row.costo))
    setEstado(row.estado === 'ACTIVO' ? 'ACTIVO' : 'INACTIVO')
    setError(null)
  }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = {
      leagueId,
      descripcion: descripcion.trim(),
      costo: Number(costo),
      estado,
    }
    try {
      if (editing) {
        await api(`/api/sanciones-jugador/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/sanciones-jugador', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      await cargar(filtroDescripcion, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar la sanción')
    }
  }

  async function eliminar(row: Sancion) {
    setError(null)
    try {
      await api(`/api/sanciones-jugador/${row.id}`, { method: 'DELETE' })
      if (editing?.id === row.id) reset()
      await cargar(filtroDescripcion, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar la sanción')
    }
  }

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
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
            <th>Costo</th>
            <th>Estado</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.descripcion}</td>
              <td>{dinero(row.costo)}</td>
              <td>{etiqueta(row.estado)}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => editar(row)}>Editar</button>
                  <button type="button" className="ghost" onClick={() => eliminar(row)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={writable ? 4 : 3}>No hay sanciones.</td></tr>}
        </tbody>
      </table>
      {writable && leagueId && (
        <form onSubmit={guardar}>
          <h2>{editing ? 'Editar sanción' : 'Nueva sanción'}</h2>
          <label>
            Descripción
            <input value={descripcion} maxLength={160} required onChange={(event) => setDescripcion(event.target.value)} />
          </label>
          <label>
            Costo
            <input type="number" min={0} step="0.01" required value={costo} onChange={(event) => setCosto(event.target.value)} />
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
        </form>
      )}
    </section>
  )
}
