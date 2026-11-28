import { useEffect, useRef, useState, type ChangeEvent, type FormEvent, type MouseEvent } from 'react'
import { api } from './api'
import { IconoPdf } from './DescargarImagen'
import { aDia, FechaCampo } from './Fecha'
import { useFocoEdicion } from './foco'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type League = {
  id: string
  name: string
  validFrom: string
  validUntil: string
  active: boolean
  hasLogo: boolean
  imagenPlantilla: string
}

type Edition = {
  id: string
  leagueId: string
  leagueName: string
  name: string
  startsOn: string
  endsOn: string
}

type Category = {
  id: string
  editionId: string
  editionName: string
  leagueId: string
  leagueName: string
  name: string
  maxPlayers: number
  cantidadGrupos: number
  equiposPorGrupo: number | null
  cantidadFases: number
  valorPagoPartido: number
  valorCobroEquipo: number
  valorPagoSinPito: number
  estado: string
}

type Team = {
  id: string
  leagueId: string
  leagueName: string
  name: string
  nombreRepresentante: string | null
  numeroTelefono: string | null
  estado: string
  hasLogo: boolean
  totalJugadores: number
  categorias?: { id: string; nombre: string }[]
}

function etiquetaEstado(estado: string) {
  if (estado === 'BLOQUEADO') return 'Bloqueado'
  if (estado === 'INACTIVO') return 'Inactivo'
  if (estado === 'ELIMINADO') return 'Eliminado'
  return 'Activo'
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

function LogoEquipo({ id, hasLogo, stamp, grande = false }: { id?: string; hasLogo: boolean; stamp: number; grande?: boolean }) {
  if (hasLogo && id) {
    return <img className={grande ? 'team-logo preview' : 'team-logo'} alt="" src={`/api/teams/${id}/logo?v=${stamp}`} />
  }
  return (
    <span className={grande ? 'team-logo vacio preview' : 'team-logo vacio'} aria-label="Sin logo">
      <svg viewBox="0 0 64 64" aria-hidden="true">
        <path d="M32 6l20 8v16c0 14-8 24-20 28C20 54 12 44 12 30V14z" fill="none" stroke="currentColor" strokeWidth="3" />
      </svg>
    </span>
  )
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((membership) => membership.role === 'ADMINISTRATIVO')
}

type ScopeChange = (next?: { leagueId?: string; editionId?: string }) => void

export function LeagueScreen({ profile, onScopeChange }: { profile: Profile; leagueId: string; onScopeChange: ScopeChange }) {
  const [rows, setRows] = useState<League[]>([])
  const [editing, setEditing] = useState<League | null>(null)
  const formEdicion = useFocoEdicion(editing?.id ?? null)
  const [name, setName] = useState('')
  const [validFrom, setValidFrom] = useState('')
  const [validUntil, setValidUntil] = useState('')
  const [active, setActive] = useState(true)
  const [logo, setLogo] = useState<string | null>(null)
  const [logoType, setLogoType] = useState<string | null>(null)
  const [clearLogo, setClearLogo] = useState(false)
  const [imagenPlantilla, setImagenPlantilla] = useState('fondo-default.jpg')
  const [logoStamp, setLogoStamp] = useState(0)
  const [filtroNombre, setFiltroNombre] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVA')
  const [consultaNombre, setConsultaNombre] = useState('')
  const [consultaEstado, setConsultaEstado] = useState('ACTIVA')
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)
  const ligas = rows.filter((row) => {
    const texto = consultaNombre.trim().toLowerCase()
    if (texto && !row.name.toLowerCase().includes(texto)) {
      return false
    }
    if (consultaEstado === 'ACTIVA') {
      return row.active
    }
    if (consultaEstado === 'INACTIVA') {
      return !row.active
    }
    return true
  })

  async function load() {
    setRows(await api<League[]>('/api/leagues'))
    setLogoStamp(Date.now())
  }

  useEffect(() => {
    load().catch((reason: Error) => setError(reason.message))
  }, [])

  function reset() {
    setEditing(null)
    setName('')
    setValidFrom('')
    setValidUntil('')
    setActive(true)
    setLogo(null)
    setLogoType(null)
    setClearLogo(false)
    setImagenPlantilla('fondo-default.jpg')
  }

  function edit(row: League) {
    setEditing(row)
    setName(row.name)
    setValidFrom(row.validFrom)
    setValidUntil(row.validUntil)
    setActive(row.active)
    setLogo(null)
    setLogoType(null)
    setClearLogo(false)
    setImagenPlantilla(row.imagenPlantilla || 'fondo-default.jpg')
  }

  function onLogo(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    if (file.size > 2_000_000) {
      setError('El logo debe pesar como máximo 2 MB')
      event.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : null
      const declared = (file.type || dataUrl?.slice(5, dataUrl.indexOf(';')) || '').toLowerCase()
      const type = declared === 'image/jpg' || declared === 'image/pjpeg' ? 'image/jpeg' : declared
      setLogo(dataUrl)
      setLogoType(type)
      setClearLogo(false)
      setError(null)
    }
    reader.readAsDataURL(file)
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = { name, validFrom, validUntil, active, logoBase64: logo, logoContentType: logoType, clearLogo, imagenPlantilla }
    try {
      const saved = editing
        ? await api<League>(`/api/leagues/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
        : await api<League>('/api/leagues', { method: 'POST', body: JSON.stringify(body) })
      reset()
      await load()
      onScopeChange({ leagueId: saved.id })
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar')
    }
  }

  async function remove(row: League) {
    setError(null)
    try {
      await api(`/api/leagues/${row.id}`, { method: 'DELETE' })
      if (editing?.id === row.id) {
        reset()
      }
      await load()
      onScopeChange()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar')
    }
  }

  function consultar(event: FormEvent) {
    event.preventDefault()
    setConsultaNombre(filtroNombre)
    setConsultaEstado(filtroEstado)
  }

  return (
    <section className="catalog">
      <form className="filtros" onSubmit={consultar}>
        <label>
          Nombre
          <input value={filtroNombre} onChange={(event) => setFiltroNombre(event.target.value)} />
        </label>
        <label>
          Estado
          <select value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value)}>
            <option value="ACTIVA">Activa</option>
            <option value="INACTIVA">Inactiva</option>
            <option value="">Todos</option>
          </select>
        </label>
        <button type="submit">Consultar</button>
      </form>
      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Desde</th>
            <th>Hasta</th>
            <th>Estado</th>
            <th>Logo</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {ligas.map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{aDia(row.validFrom)}</td>
              <td>{aDia(row.validUntil)}</td>
              <td>{row.active ? 'Activa' : 'Inactiva'}</td>
              <td>
                {row.hasLogo && (
                  <img className="league-logo" alt="" src={`/api/leagues/${row.id}/logo?v=${logoStamp}`} />
                )}
              </td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => edit(row)}>Editar</button>
                  {profile.master && <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>}
                </td>
              )}
            </tr>
          ))}
          {ligas.length === 0 && (
            <tr>
              <td colSpan={writable ? 6 : 5}>No hay ligas.</td>
            </tr>
          )}
        </tbody>
      </table>
      {(profile.master || editing) && writable && (
        <form ref={formEdicion} onSubmit={onSubmit}>
          <h2>{editing ? 'Editar liga' : 'Nueva liga'}</h2>
          <label>
            Nombre
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label>
            Vigencia desde
            <FechaCampo value={validFrom || null} required onChange={(fecha) => setValidFrom(fecha ?? '')} />
          </label>
          <label>
            Vigencia hasta
            <FechaCampo value={validUntil || null} required onChange={(fecha) => setValidUntil(fecha ?? '')} />
          </label>
          <label className="check">
            <input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} />
            Activa
          </label>
          <label>
            Logo
            <input key={editing?.id ?? 'nueva'} type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={onLogo} />
          </label>
          {(logo || (editing?.hasLogo && !clearLogo)) && (
            <img
              className="league-logo preview"
              alt=""
              src={logo ?? `/api/leagues/${editing?.id}/logo?v=${logoStamp}`}
            />
          )}
          {editing?.hasLogo && (
            <label className="check">
              <input type="checkbox" checked={clearLogo} onChange={(event) => setClearLogo(event.target.checked)} />
              Quitar logo
            </label>
          )}
          <label>
            Imagen plantilla
            <input value={imagenPlantilla} required onChange={(event) => setImagenPlantilla(event.target.value)} />
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

export function EditionScreen({ profile, leagueId, onScopeChange }: { profile: Profile; leagueId: string; onScopeChange: ScopeChange }) {
  const [rows, setRows] = useState<Edition[]>([])
  const [editing, setEditing] = useState<Edition | null>(null)
  const formEdicion = useFocoEdicion(editing?.id ?? null)
  const [name, setName] = useState('')
  const [startsOn, setStartsOn] = useState('')
  const [endsOn, setEndsOn] = useState('')
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  useEffect(() => {
    if (!leagueId) {
      setRows([])
      return
    }
    api<Edition[]>(`/api/editions?leagueId=${leagueId}`)
      .then(setRows)
      .catch((reason: Error) => setError(reason.message))
  }, [leagueId])

  function reset() {
    setEditing(null)
    setName('')
    setStartsOn('')
    setEndsOn('')
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = { leagueId, name, startsOn, endsOn }
    try {
      const saved = editing
        ? await api<Edition>(`/api/editions/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
        : await api<Edition>('/api/editions', { method: 'POST', body: JSON.stringify(body) })
      reset()
      setRows(await api<Edition[]>(`/api/editions?leagueId=${leagueId}`))
      onScopeChange({ editionId: saved.id })
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar')
    }
  }

  async function remove(row: Edition) {
    setError(null)
    try {
      await api(`/api/editions/${row.id}`, { method: 'DELETE' })
      setRows(await api<Edition[]>(`/api/editions?leagueId=${leagueId}`))
      onScopeChange()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar')
    }
  }

  return (
    <section className="catalog">
      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Inicio</th>
            <th>Fin</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{aDia(row.startsOn)}</td>
              <td>{aDia(row.endsOn)}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => { setEditing(row); setName(row.name); setStartsOn(row.startsOn); setEndsOn(row.endsOn) }}>Editar</button>
                  <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={4}>No hay ediciones.</td></tr>}
        </tbody>
      </table>
      {writable && (
        <form ref={formEdicion} onSubmit={onSubmit}>
          <h2>{editing ? 'Editar edición' : 'Nueva edición'}</h2>
          <label>Nombre<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
          <label>Inicio<FechaCampo value={startsOn || null} required onChange={(fecha) => setStartsOn(fecha ?? '')} /></label>
          <label>Fin<FechaCampo value={endsOn || null} required onChange={(fecha) => setEndsOn(fecha ?? '')} /></label>
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

export function CategoryScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [rows, setRows] = useState<Category[]>([])
  const [editing, setEditing] = useState<Category | null>(null)
  const formEdicion = useFocoEdicion(editing?.id ?? null)
  const [name, setName] = useState('')
  const [maxPlayers, setMaxPlayers] = useState('25')
  const [cantidadGrupos, setCantidadGrupos] = useState('1')
  const [equiposPorGrupo, setEquiposPorGrupo] = useState('')
  const [cantidadFases, setCantidadFases] = useState('1')
  const [valorPagoPartido, setValorPagoPartido] = useState('0.00')
  const [valorCobroEquipo, setValorCobroEquipo] = useState('0.00')
  const [valorPagoSinPito, setValorPagoSinPito] = useState('0.00')
  const [estado, setEstado] = useState('ACTIVO')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  useEffect(() => {
    if (!editionId) {
      setRows([])
      return
    }
    setFiltroEstado('ACTIVO')
    api<Category[]>(`/api/categories?editionId=${editionId}&estado=ACTIVO`)
      .then(setRows)
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  function reset() {
    setEditing(null)
    setName('')
    setMaxPlayers('25')
    setCantidadGrupos('1')
    setEquiposPorGrupo('')
    setCantidadFases('1')
    setValorPagoPartido('0.00')
    setValorCobroEquipo('0.00')
    setValorPagoSinPito('0.00')
    setEstado('ACTIVO')
  }

  async function reload(estadoFiltro = filtroEstado) {
    const params = new URLSearchParams({ editionId })
    if (estadoFiltro) params.set('estado', estadoFiltro)
    else params.set('estado', 'TODOS')
    setRows(await api<Category[]>(`/api/categories?${params.toString()}`))
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const equipos = equiposPorGrupo.trim()
    const body = {
      editionId,
      name,
      maxPlayers: Number(maxPlayers),
      cantidadGrupos: Number(cantidadGrupos),
      equiposPorGrupo: equipos === '' ? null : Number(equipos),
      cantidadFases: Number(cantidadFases),
      valorPagoPartido: Number(valorPagoPartido),
      valorCobroEquipo: Number(valorCobroEquipo),
      valorPagoSinPito: Number(valorPagoSinPito),
      estado,
    }
    try {
      if (editing) {
        await api(`/api/categories/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/categories', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      await reload()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar')
    }
  }

  async function remove(row: Category) {
    setError(null)
    try {
      await api(`/api/categories/${row.id}`, { method: 'DELETE' })
      await reload()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar')
    }
  }

  return (
    <section className="catalog">
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Estado
            <select value={filtroEstado} onChange={(event) => { setFiltroEstado(event.target.value); reload(event.target.value).catch((reason: Error) => setError(reason.message)) }}>
              <option value="ACTIVO">Activa</option>
              <option value="INACTIVO">Inactiva</option>
              <option value="">Todos</option>
            </select>
          </label>
        </form>
      )}
      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Jugadores</th>
            <th>Grupos</th>
            <th>Equipos por grupo</th>
            <th>Fases</th>
            <th>Estado</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{row.maxPlayers}</td>
              <td>{row.cantidadGrupos}</td>
              <td>{row.equiposPorGrupo ?? 'Todos'}</td>
              <td>{row.cantidadFases}</td>
              <td>{row.estado === 'INACTIVO' ? 'Inactiva' : 'Activa'}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => { setEditing(row); setName(row.name); setMaxPlayers(String(row.maxPlayers)); setCantidadGrupos(String(row.cantidadGrupos)); setEquiposPorGrupo(row.equiposPorGrupo == null ? '' : String(row.equiposPorGrupo)); setCantidadFases(String(row.cantidadFases)); setValorPagoPartido(Number(row.valorPagoPartido ?? 0).toFixed(2)); setValorCobroEquipo(Number(row.valorCobroEquipo ?? 0).toFixed(2)); setValorPagoSinPito(Number(row.valorPagoSinPito ?? 0).toFixed(2)); setEstado(row.estado === 'INACTIVO' ? 'INACTIVO' : 'ACTIVO') }}>Editar</button>
                  {row.estado !== 'INACTIVO' && <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>}
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={writable ? 7 : 6}>No hay categorías.</td></tr>}
        </tbody>
      </table>
      {writable && editionId && (
        <form ref={formEdicion} onSubmit={onSubmit}>
          <h2>{editing ? 'Editar categoría' : 'Nueva categoría'}</h2>
          <label>Nombre<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
          <label>Máximo de jugadores<input type="number" min={1} value={maxPlayers} onChange={(event) => setMaxPlayers(event.target.value)} required /></label>
          <label>Grupos<input type="number" min={1} value={cantidadGrupos} onChange={(event) => setCantidadGrupos(event.target.value)} required /></label>
          <label>Equipos por grupo<input type="number" min={1} value={equiposPorGrupo} onChange={(event) => setEquiposPorGrupo(event.target.value)} placeholder="Todos" /></label>
          <label>Fases<input type="number" min={1} value={cantidadFases} onChange={(event) => setCantidadFases(event.target.value)} required /></label>
          <fieldset className="grupo-arbitraje">
            <legend>Arbitraje</legend>
            <label>
              Valor cobro equipo
              <input type="number" min={0} step="0.01" required value={valorCobroEquipo} onChange={(event) => setValorCobroEquipo(event.target.value)} />
            </label>
            <label>
              Valor a pagar arbitraje
              <input type="number" min={0} step="0.01" required value={valorPagoPartido} onChange={(event) => setValorPagoPartido(event.target.value)} />
            </label>
            <label>
              Valor a pagar si árbitro no pita
              <input type="number" min={0} step="0.01" required value={valorPagoSinPito} onChange={(event) => setValorPagoSinPito(event.target.value)} />
            </label>
          </fieldset>
          <label>
            Estado
            <select value={estado} onChange={(event) => setEstado(event.target.value)}>
              <option value="ACTIVO">Activa</option>
              <option value="INACTIVO">Inactiva</option>
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

export function TeamScreen({ profile, leagueId, editionId }: { profile: Profile; leagueId: string; editionId: string }) {
  const [rows, setRows] = useState<Team[]>([])
  const [editing, setEditing] = useState<Team | null>(null)
  const formEdicion = useFocoEdicion(editing?.id ?? null)
  const [name, setName] = useState('')
  const [nombreRepresentante, setNombreRepresentante] = useState('')
  const [numeroTelefono, setNumeroTelefono] = useState('')
  const [estado, setEstado] = useState('ACTIVO')
  const [logo, setLogo] = useState<string | null>(null)
  const [logoType, setLogoType] = useState<string | null>(null)
  const [clearLogo, setClearLogo] = useState(false)
  const [logoStamp, setLogoStamp] = useState(0)
  const [filtroNombre, setFiltroNombre] = useState('')
  const [filtroRepresentante, setFiltroRepresentante] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [categorias, setCategorias] = useState<Category[]>([])
  const [categoriaId, setCategoriaId] = useState('')
  const [categoriaActual, setCategoriaActual] = useState('')
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  function equiposUrl(nombre: string, representante: string, estadoEquipo: string, categoria: string) {
    const params = new URLSearchParams({ leagueId })
    if (editionId) params.set('editionId', editionId)
    if (nombre.trim()) params.set('nombre', nombre.trim())
    if (representante.trim()) params.set('representante', representante.trim())
    if (categoria) params.set('categoryId', categoria)
    params.set('estado', estadoEquipo)
    return `/api/teams?${params.toString()}`
  }

  useEffect(() => {
    setFiltroNombre('')
    setFiltroRepresentante('')
    setFiltroEstado('ACTIVO')
    setFiltroCategoria('')
    if (!leagueId || !editionId) {
      setRows([])
      return
    }
    const params = new URLSearchParams({ leagueId, estado: 'ACTIVO', editionId })
    api<Team[]>(`/api/teams?${params.toString()}`)
      .then((items) => {
        setRows(items)
        setLogoStamp(Date.now())
      })
      .catch((reason: Error) => setError(reason.message))
  }, [leagueId, editionId])

  useEffect(() => {
    if (!editionId) {
      setCategorias([])
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then(setCategorias)
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  function reset() {
    setEditing(null)
    setName('')
    setNombreRepresentante('')
    setNumeroTelefono('')
    setEstado('ACTIVO')
    setLogo(null)
    setLogoType(null)
    setClearLogo(false)
    setCategoriaId('')
    setCategoriaActual('')
  }

  function edit(row: Team) {
    setEditing(row)
    setName(row.name)
    setNombreRepresentante(row.nombreRepresentante ?? '')
    setNumeroTelefono(row.numeroTelefono ?? '')
    setEstado(row.estado === 'BLOQUEADO' || row.estado === 'INACTIVO' ? row.estado : 'ACTIVO')
    setLogo(null)
    setLogoType(null)
    setClearLogo(false)
    const inscritas = row.categorias ?? []
    setCategoriaActual(inscritas.length > 1 ? inscritas[0].id : '')
    setCategoriaId(inscritas.length === 1 ? inscritas[0].id : '')
  }

  function onLogo(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    if (file.size > 2_000_000) {
      setError('El logo debe pesar como máximo 2 MB')
      event.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : null
      const declared = (file.type || dataUrl?.slice(5, dataUrl.indexOf(';')) || '').toLowerCase()
      const type = declared === 'image/jpg' || declared === 'image/pjpeg' ? 'image/jpeg' : declared
      setLogo(dataUrl)
      setLogoType(type)
      setClearLogo(false)
      setError(null)
    }
    reader.readAsDataURL(file)
  }

  async function reload() {
    setRows(await api<Team[]>(equiposUrl(filtroNombre, filtroRepresentante, filtroEstado, filtroCategoria)))
    setLogoStamp(Date.now())
  }

  async function consultar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await reload()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudieron consultar los equipos')
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const varias = (editing?.categorias?.length ?? 0) > 1
    const body = {
      leagueId,
      name,
      nombreRepresentante,
      numeroTelefono,
      estado,
      logoBase64: logo,
      logoContentType: logoType,
      clearLogo,
      categoryId: varias ? (categoriaId || null) : (categoriaId || null),
      categoryIdActual: varias ? categoriaActual : null,
    }
    try {
      if (editing) {
        await api(`/api/teams/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/teams', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      await reload()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar')
    }
  }

  async function remove(row: Team) {
    setError(null)
    try {
      await api(`/api/teams/${row.id}`, { method: 'DELETE' })
      await reload()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar')
    }
  }

  return (
    <section className="catalog">
      {!editionId && <p>Selecciona una edición.</p>}
      <form className="filtros" onSubmit={consultar}>
        <label>Nombre<input value={filtroNombre} onChange={(event) => setFiltroNombre(event.target.value)} /></label>
        <label>Representante<input value={filtroRepresentante} onChange={(event) => setFiltroRepresentante(event.target.value)} /></label>
        <label>
          Categoría
          <select value={filtroCategoria} onChange={(event) => setFiltroCategoria(event.target.value)}>
            <option value="">Todas</option>
            {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.name}</option>)}
          </select>
        </label>
        <label>
          Estado
          <select value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value)}>
            <option value="ACTIVO">Activo</option>
            <option value="BLOQUEADO">Bloqueado</option>
            <option value="INACTIVO">Inactivo</option>
            <option value="">Todos</option>
          </select>
        </label>
        <button type="submit">Consultar</button>
      </form>
      <table>
        <thead>
          <tr>
            <th className="numero">Nº</th>
            <th>Logo</th>
            <th>Nombre</th>
            <th>Categoría</th>
            <th>Representante</th>
            <th>Teléfono</th>
            <th className="numero" title="Jugadores inscritos">JI</th>
            <th>Estado</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => {
            const whatsapp = whatsappUrl(row.numeroTelefono)
            return (
            <tr key={row.id}>
              <td className="numero">{index + 1}</td>
              <td><LogoEquipo id={row.id} hasLogo={row.hasLogo} stamp={logoStamp} /></td>
              <td>{row.name}</td>
              <td>{(row.categorias ?? []).map((categoria) => categoria.nombre).join(', ')}</td>
              <td>{row.nombreRepresentante}</td>
              <td className="telefono">
                {row.numeroTelefono}
                {whatsapp && (
                  <a className="whatsapp" href={whatsapp} target="_blank" rel="noreferrer" aria-label={`Chatear por WhatsApp con ${row.nombreRepresentante || row.name}`}>
                    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                      <path fill="currentColor" d="M12.04 2C6.58 2 2.15 6.4 2.15 11.83c0 1.74.46 3.44 1.33 4.94L2 22l5.39-1.41a10.1 10.1 0 0 0 4.65 1.12h.01c5.46 0 9.89-4.4 9.89-9.83C21.94 6.4 17.5 2 12.04 2zm5.76 13.89c-.24.68-1.4 1.3-1.94 1.38-.5.08-1.13.11-1.82-.11-.42-.14-.95-.31-1.64-.6-2.88-1.24-4.76-4.14-4.9-4.33-.14-.19-1.16-1.54-1.16-2.94s.73-2.08 1-2.37c.24-.27.64-.39.86-.39h.62c.2 0 .46-.07.72.55.26.64.9 2.2.98 2.36.08.16.13.35.03.56-.1.22-.15.35-.3.54-.15.19-.31.42-.44.56-.15.15-.3.32-.13.62.16.3.73 1.2 1.57 1.95 1.08.96 1.99 1.26 2.27 1.4.28.14.45.12.61-.07.17-.19.7-.81.89-1.09.19-.28.38-.23.64-.14.26.08 1.66.78 1.95.92.28.15.47.22.54.34.07.12.07.7-.17 1.38z" />
                    </svg>
                  </a>
                )}
              </td>
              <td className="numero">{row.totalJugadores ?? 0}</td>
              <td>{etiquetaEstado(row.estado)}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => edit(row)}>Editar</button>
                  <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>
                </td>
              )}
            </tr>
            )
          })}
          {rows.length === 0 && <tr><td colSpan={writable ? 9 : 8}>No hay equipos.</td></tr>}
        </tbody>
      </table>
      {writable && (
        <form ref={formEdicion} onSubmit={onSubmit}>
          <h2>{editing ? 'Editar equipo' : 'Nuevo equipo'}</h2>
          <label>Nombre<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
          <label>Nombre de representante<input value={nombreRepresentante} maxLength={160} onChange={(event) => setNombreRepresentante(event.target.value)} /></label>
          <label>Número de teléfono<input value={numeroTelefono} maxLength={30} onChange={(event) => setNumeroTelefono(event.target.value)} /></label>
          {(editing?.categorias?.length ?? 0) > 1 ? (
            <>
              <label>
                Categoría actual
                <select value={categoriaActual} onChange={(event) => setCategoriaActual(event.target.value)}>
                  {(editing?.categorias ?? []).map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}
                </select>
              </label>
              <label>
                Nueva categoría
                <select value={categoriaId} onChange={(event) => setCategoriaId(event.target.value)}>
                  <option value="">Sin cambio</option>
                  {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.name}</option>)}
                </select>
              </label>
            </>
          ) : (
            <label>
              Categoría
              <select value={categoriaId} onChange={(event) => setCategoriaId(event.target.value)}>
                {(!editing || (editing.categorias?.length ?? 0) === 0) && <option value="">Sin categoría</option>}
                {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.name}</option>)}
              </select>
            </label>
          )}
          <label>
            Estado
            <select value={estado} onChange={(event) => setEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="BLOQUEADO">Bloqueado</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <label>
            Logo
            <input key={editing?.id ?? 'nuevo'} type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={onLogo} />
          </label>
          {logo
            ? <img className="team-logo preview" alt="" src={logo} />
            : <LogoEquipo id={editing?.id} hasLogo={Boolean(editing?.hasLogo) && !clearLogo} stamp={logoStamp} grande />}
          {editing?.hasLogo && (
            <label className="check">
              <input type="checkbox" checked={clearLogo} onChange={(event) => setClearLogo(event.target.checked)} />
              Quitar logo
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

type TeamEntry = {
  id: string
  teamId: string
  teamName: string
  categoryId: string
  categoryName: string
  leagueId: string
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

type Player = {
  id: string
  teamEntryId: string
  teamId: string
  teamName: string
  categoryId: string
  name: string
  alias: string | null
  cedula: string
  numeroCamisa: number
  rotationDegrees: number
  estado: string
  habilitado: boolean
  reemplazo: boolean
  hasPhoto: boolean
}

export function PlayerScreen({ profile, leagueId, editionId, equipoInicial = null }: { profile: Profile; leagueId: string; editionId: string; equipoInicial?: { categoryId: string; teamId: string } | null }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [teams, setTeams] = useState<Team[]>([])
  const [teamId, setTeamId] = useState('')
  const [entry, setEntry] = useState<TeamEntry | null>(null)
  const [rows, setRows] = useState<Player[]>([])
  const [editing, setEditing] = useState<Player | null>(null)
  const formEdicion = useFocoEdicion(editing?.id ?? null)
  const [name, setName] = useState('')
  const [alias, setAlias] = useState('')
  const [cedula, setCedula] = useState('')
  const [numeroCamisa, setNumeroCamisa] = useState('')
  const [rotation, setRotation] = useState(0)
  const [photo, setPhoto] = useState<string | null>(null)
  const [photoType, setPhotoType] = useState<string | null>(null)
  const [clearPhoto, setClearPhoto] = useState(false)
  const [photoStamp, setPhotoStamp] = useState(0)
  const [estado, setEstado] = useState('ACTIVO')
  const [filtroCedula, setFiltroCedula] = useState('')
  const [filtroNombre, setFiltroNombre] = useState('')
  const [filtroCamisa, setFiltroCamisa] = useState('')
  const [filtroRefuerzo, setFiltroRefuerzo] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('ACTIVO')
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [fotoAmpliada, setFotoAmpliada] = useState<{ id: string; rotation: number; top: number; left: number } | null>(null)
  const fotoFijaId = useRef<string | null>(null)
  const fotoCerradaEn = useRef(0)
  const writable = canWrite(profile)
  const category = categories.find((item) => item.id === categoryId)
  const equipoPendiente = useRef(equipoInicial)

  useEffect(() => {
    equipoPendiente.current = equipoInicial
  }, [equipoInicial])

  useEffect(() => {
    if (!leagueId || !editionId || !categoryId) {
      setTeams([])
      setTeamId('')
      return
    }
    let vigente = true
    api<Team[]>(`/api/teams?leagueId=${leagueId}&estado=ACTIVO&editionId=${editionId}&categoryId=${categoryId}`)
      .then((items) => {
        if (!vigente) {
          return
        }
        setTeams(items)
        const preferido = equipoPendiente.current?.categoryId === categoryId ? equipoPendiente.current.teamId : ''
        setTeamId(items.find((team) => team.id === preferido)?.id ?? items[0]?.id ?? '')
      })
      .catch((reason: Error) => {
        if (vigente) {
          setError(reason.message)
        }
      })
    return () => {
      vigente = false
    }
  }, [leagueId, editionId, categoryId])

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      setCategoryId('')
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        setCategories(items)
        const preferida = equipoPendiente.current?.categoryId
        setCategoryId(items.find((item) => item.id === preferida)?.id ?? items[0]?.id ?? '')
      })
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  useEffect(() => {
    if (!categoryId || !teamId) {
      setEntry(null)
      setRows([])
      return
    }
    api<TeamEntry[]>(`/api/team-entries?categoryId=${categoryId}`)
      .then(async (items) => {
        const found = items.find((item) => item.teamId === teamId) ?? null
        setEntry(found)
        setRows(found ? await api<Player[]>(`/api/players?teamEntryId=${found.id}&estado=TODOS`) : [])
        setPhotoStamp(Date.now())
      })
      .catch((reason: Error) => setError(reason.message))
  }, [categoryId, teamId])

  function reset() {
    setEditing(null)
    setName('')
    setAlias('')
    setCedula('')
    setNumeroCamisa('')
    setRotation(0)
    setPhoto(null)
    setPhotoType(null)
    setClearPhoto(false)
    setEstado('ACTIVO')
  }

  async function reload(nextEntry: TeamEntry) {
    setEntry(nextEntry)
    setRows(await api<Player[]>(`/api/players?teamEntryId=${nextEntry.id}&estado=TODOS`))
    setPhotoStamp(Date.now())
  }

  async function enroll() {
    setError(null)
    try {
      const created = await api<TeamEntry>('/api/team-entries', {
        method: 'POST',
        body: JSON.stringify({ teamId, categoryId }),
      })
      await reload(created)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo inscribir el equipo')
    }
  }

  function onPhoto(event: ChangeEvent<HTMLInputElement>) {
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
      setPhoto(dataUrl)
      setPhotoType(type)
      setClearPhoto(false)
      setError(null)
    }
    reader.readAsDataURL(file)
  }

  useEffect(() => {
    const value = cedula.trim()
    if (!value) {
      return
    }
    let cancelled = false
    const timer = window.setTimeout(() => {
      api<Player>(`/api/players/lookup?cedula=${encodeURIComponent(value)}`)
        .then(async (found) => {
          if (cancelled || !found || found.id === editing?.id) {
            return
          }
          setName(found.name)
          setAlias(found.alias ?? '')
          setNumeroCamisa((current) => current.trim() ? current : String(found.numeroCamisa))
          setRotation(found.rotationDegrees)
          if (!found.hasPhoto) {
            return
          }
          const response = await fetch(`/api/players/${found.id}/photo`, { credentials: 'include' })
          if (!response.ok || cancelled) {
            return
          }
          const blob = await response.blob()
          const dataUrl = await new Promise<string>((resolve, reject) => {
            const reader = new FileReader()
            reader.onload = () => resolve(String(reader.result))
            reader.onerror = () => reject(reader.error)
            reader.readAsDataURL(blob)
          })
          if (cancelled) {
            return
          }
          setPhoto(dataUrl)
          setPhotoType(blob.type || 'image/jpeg')
          setClearPhoto(false)
        })
        .catch(() => undefined)
    }, 300)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [cedula, editing?.id])

  function edit(row: Player) {
    setEditing(row)
    setName(row.name)
    setAlias(row.alias ?? '')
    setCedula(row.cedula)
    setNumeroCamisa(String(row.numeroCamisa))
    setRotation(row.rotationDegrees)
    setPhoto(null)
    setPhotoType(null)
    setClearPhoto(false)
    setEstado(row.estado === 'INACTIVO' ? 'INACTIVO' : 'ACTIVO')
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!entry) {
      return
    }
    setError(null)
    const aliasValue = alias.trim() || aliasFromName(name)
    if (aliasValue !== alias) {
      setAlias(aliasValue)
    }
    const body = {
      teamEntryId: entry.id,
      name,
      alias: aliasValue,
      cedula,
      numeroCamisa: Number(numeroCamisa),
      rotationDegrees: rotation,
      estado,
      photoBase64: photo,
      photoContentType: photoType,
      clearPhoto,
    }
    try {
      if (editing) {
        await api(`/api/players/${editing.id}`, { method: 'PUT', body: JSON.stringify(body) })
      } else {
        await api('/api/players', { method: 'POST', body: JSON.stringify(body) })
      }
      reset()
      await reload(entry)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar')
    }
  }

  function mostrarFoto(row: Player, event: MouseEvent<HTMLButtonElement>) {
    const rect = event.currentTarget.getBoundingClientRect()
    const size = 220
    let left = rect.right + 10
    if (left + size > window.innerWidth - 12) {
      left = Math.max(12, rect.left - size - 10)
    }
    let top = rect.top - 70
    if (top + size > window.innerHeight - 12) {
      top = Math.max(12, window.innerHeight - size - 12)
    }
    setFotoAmpliada({ id: row.id, rotation: row.rotationDegrees, top, left })
  }

  async function cambiarHabilitado(row: Player, habilitado: boolean) {
    const anterior = row.habilitado
    setRows((current) => current.map((item) => item.id === row.id ? { ...item, habilitado } : item))
    setError(null)
    try {
      const actualizado = await api<Player>(`/api/players/${row.id}/habilitado`, {
        method: 'PATCH',
        body: JSON.stringify({ habilitado }),
      })
      setRows((current) => current.map((item) => item.id === row.id ? { ...item, habilitado: actualizado.habilitado } : item))
    } catch (reason) {
      setRows((current) => current.map((item) => item.id === row.id ? { ...item, habilitado: anterior } : item))
      setError(reason instanceof Error ? reason.message : 'No se pudo guardar')
    }
  }

  async function remove(row: Player) {
    if (!entry) {
      return
    }
    setError(null)
    try {
      await api(`/api/players/${row.id}`, { method: 'DELETE' })
      if (editing?.id === row.id) {
        reset()
      }
      await reload(entry)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar')
    }
  }

  const preview = photo ?? (editing?.hasPhoto && !clearPhoto ? `/api/players/${editing.id}/photo?v=${photoStamp}` : null)
  const cedulaBuscada = filtroCedula.trim().toLowerCase()
  const nombreBuscado = filtroNombre.trim().toLowerCase()
  const camisaBuscada = filtroCamisa.trim()
  const visibles = rows.filter((row) => {
    if (filtroEstado && row.estado !== filtroEstado) return false
    if (cedulaBuscada && !row.cedula.toLowerCase().includes(cedulaBuscada)) return false
    if (nombreBuscado && !row.name.toLowerCase().includes(nombreBuscado)) return false
    if (camisaBuscada && !String(row.numeroCamisa).includes(camisaBuscada)) return false
    if (filtroRefuerzo === 'SI' && !row.reemplazo) return false
    if (filtroRefuerzo === 'NO' && row.reemplazo) return false
    return true
  })

  async function descargarJugadores() {
    if (!editionId || !categoryId) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, categoryId })
      const response = await fetch(`/api/reportes/jugadores-por-equipo?${params.toString()}`, { credentials: 'include' })
      if (!response.ok) {
        const body = await response.json().catch(() => ({ error: 'No se pudo descargar el PDF' }))
        throw new Error(body.error ?? 'No se pudo descargar el PDF')
      }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = 'JUGADORES-POR-EQUIPO.pdf'
      enlace.click()
      URL.revokeObjectURL(url)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo descargar el PDF')
    } finally {
      setOcupado(false)
    }
  }

  return (
    <section className="catalog">
      <div className="reloj-descargas">
        <span className="reloj-botones">
          <button type="button" className="con-icono descarga-pdf" disabled={!editionId || !categoryId || ocupado} onClick={() => void descargarJugadores()}>
            <IconoPdf />
            JUGADORES POR EQUIPO
          </button>
        </span>
      </div>
      {!editionId && <p>Selecciona una edición.</p>}
      <label>
        Categoría
        <select value={categoryId} onChange={(event) => { setCategoryId(event.target.value); reset() }}>
          {categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </label>
      <label>
        Equipo
        <select value={teamId} onChange={(event) => { setTeamId(event.target.value); reset() }}>
          {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
        </select>
      </label>
      <form className="filtros filtros-linea" onSubmit={(event) => event.preventDefault()}>
        <label>
          Nº de camisa
          <input value={filtroCamisa} onChange={(event) => setFiltroCamisa(event.target.value)} />
        </label>
        <label>
          Cédula
          <input value={filtroCedula} onChange={(event) => setFiltroCedula(event.target.value)} />
        </label>
        <label>
          Nombre
          <input value={filtroNombre} onChange={(event) => setFiltroNombre(event.target.value)} />
        </label>
        <label>
          Refuerzo
          <select value={filtroRefuerzo} onChange={(event) => setFiltroRefuerzo(event.target.value)}>
            <option value="">Todos</option>
            <option value="SI">Sí</option>
            <option value="NO">No</option>
          </select>
        </label>
      </form>
      <label>
        Estado
        <select value={filtroEstado} onChange={(event) => setFiltroEstado(event.target.value)}>
          <option value="ACTIVO">Activo</option>
          <option value="INACTIVO">Inactivo</option>
          <option value="">Todos</option>
        </select>
      </label>
      {category && <p>Máximo de jugadores: {category.maxPlayers}. Inscritos: {rows.filter((row) => row.estado === 'ACTIVO').length}.</p>}
      {categoryId && teamId && !entry && (
        <p>
          Este equipo no está inscrito en la categoría.
          {writable && <button type="button" onClick={enroll}>Inscribir</button>}
        </p>
      )}
      {entry && (
        <table>
          <thead>
            <tr>
              <th className="numero">Nº</th>
              <th>Habilitado</th>
              <th>Foto</th>
              <th>Cédula</th>
              <th>Nº de camisa</th>
              <th>Nombre</th>
              <th>Alias</th>
              <th>Refuerzo</th>
              <th>Estado</th>
              {writable && <th></th>}
            </tr>
          </thead>
          <tbody>
            {visibles.map((row, index) => (
              <tr key={row.id}>
                <td className="numero">{index + 1}</td>
                <td className="habilitado">
                  <input
                    type="checkbox"
                    checked={row.habilitado}
                    disabled={!writable}
                    aria-label={`Habilitado ${row.name}`}
                    onChange={(event) => { void cambiarHabilitado(row, event.target.checked) }}
                  />
                </td>
                <td>
                  {row.hasPhoto && (
                    <button
                      type="button"
                      className="foto-jugador"
                      aria-label={`Ampliar foto de ${row.name}`}
                      onMouseEnter={(event) => {
                        if (fotoFijaId.current === row.id || Date.now() - fotoCerradaEn.current < 400) return
                        fotoFijaId.current = null
                        mostrarFoto(row, event)
                      }}
                      onMouseLeave={() => {
                        if (fotoFijaId.current) return
                        setFotoAmpliada(null)
                      }}
                      onClick={(event) => {
                        if (fotoFijaId.current === row.id) {
                          fotoFijaId.current = null
                          fotoCerradaEn.current = Date.now()
                          setFotoAmpliada(null)
                          return
                        }
                        fotoFijaId.current = row.id
                        mostrarFoto(row, event)
                      }}
                    >
                      <img
                        className="player-photo"
                        alt=""
                        src={`/api/players/${row.id}/photo?v=${photoStamp}-${row.rotationDegrees}`}
                        style={{ transform: `rotate(${row.rotationDegrees}deg)` }}
                      />
                    </button>
                  )}
                </td>
                <td>{row.cedula}</td>
                <td>{row.numeroCamisa}</td>
                <td>{row.name}</td>
                <td>{row.alias}</td>
                <td>{row.reemplazo ? 'Sí' : 'No'}</td>
                <td>{row.estado === 'INACTIVO' ? 'Inactivo' : 'Activo'}</td>
                {writable && (
                  <td className="actions">
                    <button type="button" onClick={() => edit(row)}>Editar</button>
                    {row.estado !== 'INACTIVO' && <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>}
                  </td>
                )}
              </tr>
            ))}
            {visibles.length === 0 && <tr><td colSpan={writable ? 10 : 9}>No hay jugadores.</td></tr>}
          </tbody>
        </table>
      )}
      {fotoAmpliada && (
        <img
          className="player-photo ampliada"
          alt=""
          src={`/api/players/${fotoAmpliada.id}/photo?v=${photoStamp}-${fotoAmpliada.rotation}`}
          style={{ transform: `rotate(${fotoAmpliada.rotation}deg)`, top: fotoAmpliada.top, left: fotoAmpliada.left }}
          role="button"
          aria-label="Cerrar foto"
          onClick={() => {
            fotoFijaId.current = null
            fotoCerradaEn.current = Date.now()
            setFotoAmpliada(null)
          }}
        />
      )}
      {writable && entry && (
        <form ref={formEdicion} onSubmit={onSubmit}>
          <h2>{editing ? 'Editar jugador' : 'Nuevo jugador'}</h2>
          <label>Cédula<input value={cedula} maxLength={30} onChange={(event) => setCedula(event.target.value)} required /></label>
          <label>
            Nombre
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              onBlur={(event) => {
                if (!alias.trim()) {
                  const suggested = aliasFromName(event.target.value)
                  if (suggested) {
                    setAlias(suggested)
                  }
                }
              }}
              required
            />
          </label>
          <label>Alias<input value={alias} onChange={(event) => setAlias(event.target.value)} /></label>
          <label>Número de camisa<input type="number" min={0} value={numeroCamisa} onChange={(event) => setNumeroCamisa(event.target.value)} required /></label>
          {editing && <p>Refuerzo: {editing.reemplazo ? 'Sí' : 'No'}</p>}
          <label>
            Estado
            <select value={estado} onChange={(event) => setEstado(event.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>
          <label>
            Foto
            <input key={editing?.id ?? 'nuevo'} type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={onPhoto} />
          </label>
          {preview && (
            <img className="player-photo preview" alt="" src={preview} style={{ transform: `rotate(${rotation}deg)` }} />
          )}
          <div className="actions">
            <button type="button" className="ghost" onClick={() => setRotation((value) => (value + 90) % 360)}>Girar</button>
            {editing?.hasPhoto && (
              <label className="check">
                <input type="checkbox" checked={clearPhoto} onChange={(event) => setClearPhoto(event.target.checked)} />
                Quitar foto
              </label>
            )}
          </div>
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
