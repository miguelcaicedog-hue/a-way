import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { api } from './api'

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
}

type Team = {
  id: string
  leagueId: string
  leagueName: string
  name: string
  nombreRepresentante: string | null
  numeroTelefono: string | null
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((membership) => membership.role === 'ADMINISTRATIVO')
}

type ScopeChange = (next?: { leagueId?: string; editionId?: string }) => void

export function LeagueScreen({ profile, leagueId, onScopeChange }: { profile: Profile; leagueId: string; onScopeChange: ScopeChange }) {
  const [rows, setRows] = useState<League[]>([])
  const [editing, setEditing] = useState<League | null>(null)
  const [name, setName] = useState('')
  const [validFrom, setValidFrom] = useState('')
  const [validUntil, setValidUntil] = useState('')
  const [active, setActive] = useState(true)
  const [logo, setLogo] = useState<string | null>(null)
  const [logoType, setLogoType] = useState<string | null>(null)
  const [clearLogo, setClearLogo] = useState(false)
  const [logoStamp, setLogoStamp] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

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
    const body = { name, validFrom, validUntil, active, logoBase64: logo, logoContentType: logoType, clearLogo }
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

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
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
          {rows.filter((row) => row.id === leagueId).map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{row.validFrom}</td>
              <td>{row.validUntil}</td>
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
          {rows.filter((row) => row.id === leagueId).length === 0 && (
            <tr>
              <td colSpan={6}>No hay ligas.</td>
            </tr>
          )}
        </tbody>
      </table>
      {(profile.master || editing) && writable && (
        <form onSubmit={onSubmit}>
          <h2>{editing ? 'Editar liga' : 'Nueva liga'}</h2>
          <label>
            Nombre
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label>
            Vigencia desde
            <input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} required />
          </label>
          <label>
            Vigencia hasta
            <input type="date" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} required />
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
          <div className="actions">
            <button type="submit">Guardar</button>
            {editing && <button type="button" className="ghost" onClick={reset}>Cancelar</button>}
          </div>
        </form>
      )}
    </section>
  )
}

export function EditionScreen({ profile, leagueId, onScopeChange }: { profile: Profile; leagueId: string; onScopeChange: ScopeChange }) {
  const [rows, setRows] = useState<Edition[]>([])
  const [editing, setEditing] = useState<Edition | null>(null)
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
      {error && <p className="down">{error}</p>}
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
              <td>{row.startsOn}</td>
              <td>{row.endsOn}</td>
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
        <form onSubmit={onSubmit}>
          <h2>{editing ? 'Editar edición' : 'Nueva edición'}</h2>
          <label>Nombre<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
          <label>Inicio<input type="date" value={startsOn} onChange={(event) => setStartsOn(event.target.value)} required /></label>
          <label>Fin<input type="date" value={endsOn} onChange={(event) => setEndsOn(event.target.value)} required /></label>
          <div className="actions">
            <button type="submit">Guardar</button>
            {editing && <button type="button" className="ghost" onClick={reset}>Cancelar</button>}
          </div>
        </form>
      )}
    </section>
  )
}

export function CategoryScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [rows, setRows] = useState<Category[]>([])
  const [editing, setEditing] = useState<Category | null>(null)
  const [name, setName] = useState('')
  const [maxPlayers, setMaxPlayers] = useState('25')
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  useEffect(() => {
    if (!editionId) {
      setRows([])
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then(setRows)
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  function reset() {
    setEditing(null)
    setName('')
    setMaxPlayers('25')
  }

  async function reload() {
    setRows(await api<Category[]>(`/api/categories?editionId=${editionId}`))
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = { editionId, name, maxPlayers: Number(maxPlayers) }
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
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Jugadores</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{row.maxPlayers}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => { setEditing(row); setName(row.name); setMaxPlayers(String(row.maxPlayers)) }}>Editar</button>
                  <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={3}>No hay categorías.</td></tr>}
        </tbody>
      </table>
      {writable && editionId && (
        <form onSubmit={onSubmit}>
          <h2>{editing ? 'Editar categoría' : 'Nueva categoría'}</h2>
          <label>Nombre<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
          <label>Máximo de jugadores<input type="number" min={1} value={maxPlayers} onChange={(event) => setMaxPlayers(event.target.value)} required /></label>
          <div className="actions">
            <button type="submit">Guardar</button>
            {editing && <button type="button" className="ghost" onClick={reset}>Cancelar</button>}
          </div>
        </form>
      )}
    </section>
  )
}

export function TeamScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [rows, setRows] = useState<Team[]>([])
  const [editing, setEditing] = useState<Team | null>(null)
  const [name, setName] = useState('')
  const [nombreRepresentante, setNombreRepresentante] = useState('')
  const [numeroTelefono, setNumeroTelefono] = useState('')
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  useEffect(() => {
    if (!leagueId) {
      setRows([])
      return
    }
    api<Team[]>(`/api/teams?leagueId=${leagueId}`)
      .then(setRows)
      .catch((reason: Error) => setError(reason.message))
  }, [leagueId])

  function reset() {
    setEditing(null)
    setName('')
    setNombreRepresentante('')
    setNumeroTelefono('')
  }

  function edit(row: Team) {
    setEditing(row)
    setName(row.name)
    setNombreRepresentante(row.nombreRepresentante ?? '')
    setNumeroTelefono(row.numeroTelefono ?? '')
  }

  async function reload() {
    setRows(await api<Team[]>(`/api/teams?leagueId=${leagueId}`))
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const body = { leagueId, name, nombreRepresentante, numeroTelefono }
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
      {error && <p className="down">{error}</p>}
      <table>
        <thead>
          <tr>
            <th className="numero">Nº</th>
            <th>Nombre</th>
            <th>Representante</th>
            <th>Teléfono</th>
            {writable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id}>
              <td className="numero">{index + 1}</td>
              <td>{row.name}</td>
              <td>{row.nombreRepresentante}</td>
              <td>{row.numeroTelefono}</td>
              {writable && (
                <td className="actions">
                  <button type="button" onClick={() => edit(row)}>Editar</button>
                  <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>
                </td>
              )}
            </tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={writable ? 5 : 4}>No hay equipos.</td></tr>}
        </tbody>
      </table>
      {writable && (
        <form onSubmit={onSubmit}>
          <h2>{editing ? 'Editar equipo' : 'Nuevo equipo'}</h2>
          <label>Nombre<input value={name} onChange={(event) => setName(event.target.value)} required /></label>
          <label>Nombre de representante<input value={nombreRepresentante} maxLength={160} onChange={(event) => setNombreRepresentante(event.target.value)} /></label>
          <label>Número de teléfono<input value={numeroTelefono} maxLength={30} onChange={(event) => setNumeroTelefono(event.target.value)} /></label>
          <div className="actions">
            <button type="submit">Guardar</button>
            {editing && <button type="button" className="ghost" onClick={reset}>Cancelar</button>}
          </div>
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
  hasPhoto: boolean
}

export function PlayerScreen({ profile, leagueId, editionId }: { profile: Profile; leagueId: string; editionId: string }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [teams, setTeams] = useState<Team[]>([])
  const [teamId, setTeamId] = useState('')
  const [entry, setEntry] = useState<TeamEntry | null>(null)
  const [rows, setRows] = useState<Player[]>([])
  const [editing, setEditing] = useState<Player | null>(null)
  const [name, setName] = useState('')
  const [alias, setAlias] = useState('')
  const [cedula, setCedula] = useState('')
  const [numeroCamisa, setNumeroCamisa] = useState('')
  const [rotation, setRotation] = useState(0)
  const [photo, setPhoto] = useState<string | null>(null)
  const [photoType, setPhotoType] = useState<string | null>(null)
  const [clearPhoto, setClearPhoto] = useState(false)
  const [photoStamp, setPhotoStamp] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)
  const category = categories.find((item) => item.id === categoryId)

  useEffect(() => {
    if (!leagueId) {
      setTeams([])
      setTeamId('')
      return
    }
    api<Team[]>(`/api/teams?leagueId=${leagueId}`)
      .then((items) => {
        setTeams(items)
        setTeamId(items[0]?.id ?? '')
      })
      .catch((reason: Error) => setError(reason.message))
  }, [leagueId])

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      setCategoryId('')
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        setCategories(items)
        setCategoryId(items[0]?.id ?? '')
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
        setRows(found ? await api<Player[]>(`/api/players?teamEntryId=${found.id}`) : [])
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
  }

  async function reload(nextEntry: TeamEntry) {
    setEntry(nextEntry)
    setRows(await api<Player[]>(`/api/players?teamEntryId=${nextEntry.id}`))
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

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
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
      {category && <p>Máximo de jugadores: {category.maxPlayers}. Inscritos: {rows.length}.</p>}
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
              <th>Foto</th>
              <th>Cédula</th>
              <th>Nº de camisa</th>
              <th>Nombre</th>
              <th>Alias</th>
              {writable && <th></th>}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={row.id}>
                <td className="numero">{index + 1}</td>
                <td>
                  {row.hasPhoto && (
                    <img
                      className="player-photo"
                      alt=""
                      src={`/api/players/${row.id}/photo?v=${photoStamp}-${row.rotationDegrees}`}
                      style={{ transform: `rotate(${row.rotationDegrees}deg)` }}
                    />
                  )}
                </td>
                <td>{row.cedula}</td>
                <td>{row.numeroCamisa}</td>
                <td>{row.name}</td>
                <td>{row.alias}</td>
                {writable && (
                  <td className="actions">
                    <button type="button" onClick={() => edit(row)}>Editar</button>
                    <button type="button" className="ghost" onClick={() => remove(row)}>Eliminar</button>
                  </td>
                )}
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={writable ? 7 : 6}>No hay jugadores.</td></tr>}
          </tbody>
        </table>
      )}
      {writable && entry && (
        <form onSubmit={onSubmit}>
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
          <label>Número de camisa<input type="number" min={1} value={numeroCamisa} onChange={(event) => setNumeroCamisa(event.target.value)} required /></label>
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
        </form>
      )}
    </section>
  )
}
