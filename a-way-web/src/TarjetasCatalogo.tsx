import { useEffect, useState, type FormEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Tarjeta = {
  id: string
  leagueId: string
  descripcion: string
  color: string
  costo: number
  cantidadJornadas: number
  pesoFairPlay: number
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

export function TarjetasCatalogoScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [rows, setRows] = useState<Tarjeta[]>([])
  const [filtroDescripcion, setFiltroDescripcion] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [descripcion, setDescripcion] = useState('')
  const [color, setColor] = useState('#F5C518')
  const [costo, setCosto] = useState('0')
  const [jornadas, setJornadas] = useState('0')
  const [peso, setPeso] = useState('1')
  const [estado, setEstado] = useState('ACTIVO')
  const [editing, setEditing] = useState<Tarjeta | null>(null)
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  function url(texto: string, estadoFiltro: string) {
    const params = new URLSearchParams({ leagueId })
    if (texto.trim()) params.set('descripcion', texto.trim())
    if (estadoFiltro) params.set('estado', estadoFiltro)
    return `/api/tarjetas?${params.toString()}`
  }

  function cargar(texto: string, estadoFiltro: string) {
    return api<Tarjeta[]>(url(texto, estadoFiltro)).then(setRows)
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
    setColor('#F5C518')
    setCosto('0')
    setJornadas('0')
    setPeso('1')
    setEstado('ACTIVO')
  }

  function editar(row: Tarjeta) {
    setEditing(row)
    setDescripcion(row.descripcion)
    setColor(row.color.toLowerCase())
    setCosto(dinero(row.costo))
    setJornadas(String(row.cantidadJornadas))
    setPeso(String(row.pesoFairPlay ?? 1))
    setEstado(row.estado === 'ACTIVO' ? 'ACTIVO' : 'INACTIVO')
    setError(null)
  }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = {
      leagueId,
      descripcion: descripcion.trim(),
      color,
      costo: Number(costo),
      cantidadJornadas: Number(jornadas),
      pesoFairPlay: Number(peso),
      estado,
    }
    try {
      if (editing) {
        await api(`/api/tarjetas/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/tarjetas', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      await cargar(filtroDescripcion, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar la tarjeta')
    }
  }

  async function eliminar(row: Tarjeta) {
    setError(null)
    try {
      await api(`/api/tarjetas/${row.id}`, { method: 'DELETE' })
      if (editing?.id === row.id) reset()
      await cargar(filtroDescripcion, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar la tarjeta')
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
            <th>Color</th>
            <th>Costo</th>
            <th>Jornadas de suspensión</th>
            <th>Peso Fair Play</th>
            <th>Estado</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.descripcion}</td>
              <td><span className="tarjeta-color" style={{ background: row.color }} aria-label={row.color} /></td>
              <td>{dinero(row.costo)}</td>
              <td>{row.cantidadJornadas}</td>
              <td>{row.pesoFairPlay}</td>
              <td>{etiqueta(row.estado)}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => editar(row)}>Editar</button>
                  <button type="button" className="ghost" onClick={() => eliminar(row)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={writable ? 7 : 6}>No hay tarjetas.</td></tr>}
        </tbody>
      </table>
      {writable && leagueId && (
        <form onSubmit={guardar}>
          <h2>{editing ? 'Editar tarjeta' : 'Nueva tarjeta'}</h2>
          <label>
            Descripción
            <input value={descripcion} maxLength={160} required onChange={(event) => setDescripcion(event.target.value)} />
          </label>
          <label>
            Color
            <input type="color" value={color} onChange={(event) => setColor(event.target.value)} />
          </label>
          <label>
            Costo
            <input type="number" min={0} step="0.01" required value={costo} onChange={(event) => setCosto(event.target.value)} />
          </label>
          <label>
            Jornadas de suspensión
            <input type="number" min={0} step={1} required value={jornadas} onChange={(event) => setJornadas(event.target.value)} />
          </label>
          <label>
            Peso Fair Play
            <input type="number" min={0} step={1} required value={peso} onChange={(event) => setPeso(event.target.value)} />
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
