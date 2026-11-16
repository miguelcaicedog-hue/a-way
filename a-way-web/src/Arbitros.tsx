import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Arbitro = {
  id: string
  leagueId: string
  cedula: string | null
  nombre: string
  telefono: string | null
  valorPagoPartido: number
  valorCobroEquipo: number
  valorPagoSinPito: number
  hasFoto: boolean
  estado: string
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

function dinero(valor: number) {
  return Number(valor ?? 0).toFixed(2)
}

function etiqueta(estado: string) {
  return estado === 'ACTIVO' ? 'Activo' : 'Inactivo'
}

function whatsappUrl(phone: string | null) {
  let number = (phone ?? '').replace(/\D/g, '')
  if (number.startsWith('00')) {
    number = number.slice(2)
  }
  if (number.startsWith('0') && number.length === 10) {
    number = `593${number.slice(1)}`
  } else if (number.length === 9 && number.startsWith('9')) {
    number = `593${number}`
  }
  return number.length >= 11 ? `https://wa.me/${number}` : null
}

function FotoArbitro({ id, hasFoto, stamp, grande = false }: { id?: string; hasFoto: boolean; stamp: number; grande?: boolean }) {
  if (hasFoto && id) {
    return <img className={grande ? 'arbitro-foto preview' : 'arbitro-foto'} alt="" src={`/api/arbitros/${id}/foto?v=${stamp}`} />
  }
  return <span className={grande ? 'arbitro-foto vacio preview' : 'arbitro-foto vacio'} aria-label="Sin foto" />
}

export function ArbitrosScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [rows, setRows] = useState<Arbitro[]>([])
  const [filtroCedula, setFiltroCedula] = useState('')
  const [filtroNombre, setFiltroNombre] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [cedula, setCedula] = useState('')
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [valorPagoPartido, setValorPagoPartido] = useState('0.00')
  const [valorCobroEquipo, setValorCobroEquipo] = useState('0.00')
  const [valorPagoSinPito, setValorPagoSinPito] = useState('0.00')
  const [estado, setEstado] = useState('ACTIVO')
  const [foto, setFoto] = useState<string | null>(null)
  const [fotoTipo, setFotoTipo] = useState<string | null>(null)
  const [quitarFoto, setQuitarFoto] = useState(false)
  const [fotoStamp, setFotoStamp] = useState(0)
  const [editing, setEditing] = useState<Arbitro | null>(null)
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  function url(cedulaFiltro: string, nombreFiltro: string, estadoFiltro: string) {
    const params = new URLSearchParams({ leagueId })
    if (cedulaFiltro.trim()) params.set('cedula', cedulaFiltro.trim())
    if (nombreFiltro.trim()) params.set('nombre', nombreFiltro.trim())
    if (estadoFiltro) params.set('estado', estadoFiltro)
    return `/api/arbitros?${params.toString()}`
  }

  function cargar(cedulaFiltro: string, nombreFiltro: string, estadoFiltro: string) {
    return api<Arbitro[]>(url(cedulaFiltro, nombreFiltro, estadoFiltro)).then(setRows)
  }

  useEffect(() => {
    if (!leagueId) {
      setRows([])
      return
    }
    cargar(filtroCedula, filtroNombre, filtroEstado).catch((reason: Error) => setError(reason.message))
  }, [leagueId])

  function consultar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    cargar(filtroCedula, filtroNombre, filtroEstado).catch((reason: Error) => setError(reason.message))
  }

  function reset() {
    setEditing(null)
    setCedula('')
    setNombre('')
    setTelefono('')
    setValorPagoPartido('0.00')
    setValorCobroEquipo('0.00')
    setValorPagoSinPito('0.00')
    setEstado('ACTIVO')
    setFoto(null)
    setFotoTipo(null)
    setQuitarFoto(false)
  }

  function editar(row: Arbitro) {
    setEditing(row)
    setCedula(row.cedula ?? '')
    setNombre(row.nombre)
    setTelefono(row.telefono ?? '')
    setValorPagoPartido(dinero(row.valorPagoPartido))
    setValorCobroEquipo(dinero(row.valorCobroEquipo))
    setValorPagoSinPito(dinero(row.valorPagoSinPito))
    setEstado(row.estado === 'ACTIVO' ? 'ACTIVO' : 'INACTIVO')
    setFoto(null)
    setFotoTipo(null)
    setQuitarFoto(false)
    setError(null)
  }

  function onFoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
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
      cedula: cedula.trim() || null,
      nombre: nombre.trim(),
      telefono: telefono.trim() || null,
      valorPagoPartido: Number(valorPagoPartido),
      valorCobroEquipo: Number(valorCobroEquipo),
      valorPagoSinPito: Number(valorPagoSinPito),
      estado,
      fotoBase64: foto,
      fotoContentType: fotoTipo,
      clearFoto: quitarFoto,
    }
    try {
      if (editing) {
        await api(`/api/arbitros/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/arbitros', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      setFotoStamp((value) => value + 1)
      await cargar(filtroCedula, filtroNombre, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar el árbitro')
    }
  }

  async function eliminar(row: Arbitro) {
    setError(null)
    try {
      await api(`/api/arbitros/${row.id}`, { method: 'DELETE' })
      if (editing?.id === row.id) reset()
      await cargar(filtroCedula, filtroNombre, filtroEstado)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar el árbitro')
    }
  }

  return (
    <section className="catalog">
      {!leagueId && <p>Selecciona una liga.</p>}
      {leagueId && (
        <form className="filtros" onSubmit={consultar}>
          <label>
            Cédula
            <input value={filtroCedula} onChange={(event) => setFiltroCedula(event.target.value)} />
          </label>
          <label>
            Nombre
            <input value={filtroNombre} onChange={(event) => setFiltroNombre(event.target.value)} />
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
            <th>Foto</th>
            <th>Cédula</th>
            <th>Nombre</th>
            <th>Teléfono</th>
            <th>Pago por partido</th>
            <th>Cobro equipo</th>
            <th>Pago si no pita</th>
            <th>Estado</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const whatsapp = whatsappUrl(row.telefono)
            return (
              <tr key={row.id}>
                <td><FotoArbitro id={row.id} hasFoto={row.hasFoto} stamp={fotoStamp} /></td>
                <td>{row.cedula}</td>
                <td>{row.nombre}</td>
                <td className="telefono">
                  {row.telefono}
                  {whatsapp && (
                    <a className="whatsapp" href={whatsapp} target="_blank" rel="noreferrer" aria-label={`Chatear por WhatsApp con ${row.nombre}`}>
                      <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                        <path fill="currentColor" d="M12.04 2C6.58 2 2.15 6.4 2.15 11.83c0 1.74.46 3.44 1.33 4.94L2 22l5.39-1.41a10.1 10.1 0 0 0 4.65 1.12h.01c5.46 0 9.89-4.4 9.89-9.83C21.94 6.4 17.5 2 12.04 2zm5.76 13.89c-.24.68-1.4 1.3-1.94 1.38-.5.08-1.13.11-1.82-.11-.42-.14-.95-.31-1.64-.6-2.88-1.24-4.76-4.14-4.9-4.33-.14-.19-1.16-1.54-1.16-2.94s.73-2.08 1-2.37c.24-.27.64-.39.86-.39h.62c.2 0 .46-.07.72.55.26.64.9 2.2.98 2.36.08.16.13.35.03.56-.1.22-.15.35-.3.54-.15.19-.31.42-.44.56-.15.15-.3.32-.13.62.16.3.73 1.2 1.57 1.95 1.08.96 1.99 1.26 2.27 1.4.28.14.45.12.61-.07.17-.19.7-.81.89-1.09.19-.28.38-.23.64-.14.26.08 1.66.78 1.95.92.28.15.47.22.54.34.07.12.07.7-.17 1.38z" />
                      </svg>
                    </a>
                  )}
                </td>
                <td>{dinero(row.valorPagoPartido)}</td>
                <td>{dinero(row.valorCobroEquipo)}</td>
                <td>{dinero(row.valorPagoSinPito)}</td>
                <td>{etiqueta(row.estado)}</td>
                {writable && (
                  <td className="actions">
                    <button type="button" onClick={() => editar(row)}>Editar</button>
                    <button type="button" className="ghost" onClick={() => eliminar(row)}>Eliminar</button>
                  </td>
                )}
              </tr>
            )
          })}
          {rows.length === 0 && <tr><td colSpan={writable ? 9 : 8}>No hay árbitros.</td></tr>}
        </tbody>
      </table>
      {writable && leagueId && (
        <form onSubmit={guardar}>
          <h2>{editing ? 'Editar árbitro' : 'Nuevo árbitro'}</h2>
          <label>
            Cédula
            <input value={cedula} maxLength={20} onChange={(event) => setCedula(event.target.value)} />
          </label>
          <label>
            Nombre
            <input value={nombre} maxLength={160} required onChange={(event) => setNombre(event.target.value)} />
          </label>
          <label>
            Teléfono
            <input value={telefono} maxLength={30} onChange={(event) => setTelefono(event.target.value)} />
          </label>
          <label>
            Valor pago por partido
            <input type="number" min={0} step="0.01" required value={valorPagoPartido} onChange={(event) => setValorPagoPartido(event.target.value)} />
          </label>
          <label>
            Valor cobro equipo
            <input type="number" min={0} step="0.01" required value={valorCobroEquipo} onChange={(event) => setValorCobroEquipo(event.target.value)} />
          </label>
          <label>
            Valor pago si no pita
            <input type="number" min={0} step="0.01" required value={valorPagoSinPito} onChange={(event) => setValorPagoSinPito(event.target.value)} />
          </label>
          <label>
            Estado
            <select value={estado} onChange={(event) => setEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <label>
            Foto
            <input key={editing?.id ?? 'nuevo'} type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={onFoto} />
          </label>
          {foto
            ? <img className="arbitro-foto preview" alt="" src={foto} />
            : <FotoArbitro id={editing?.id} hasFoto={Boolean(editing?.hasFoto) && !quitarFoto} stamp={fotoStamp} grande />}
          {editing?.hasFoto && (
            <label className="check">
              <input type="checkbox" checked={quitarFoto} onChange={(event) => setQuitarFoto(event.target.checked)} />
              Quitar foto
            </label>
          )}
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
