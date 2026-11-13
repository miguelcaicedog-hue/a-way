import { ChangeEvent, useEffect, useRef, useState } from 'react'
import { api } from './api'
import { FechaCampo } from './Fecha'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Category = {
  id: string
  name: string
}

type Grupo = {
  id: string
  nombre: string
}

type Partido = {
  id: string
  orden: number
  textoLocal: string
  textoVisita: string
  fecha: string | null
}

type Jornada = {
  id: string
  numero: number
  nombre: string
  fecha: string | null
  partidos: Partido[]
}

type Panel = {
  categoryId: string
  categoryName: string
  groupId: string | null
  grupos: Grupo[]
  jornadas: Jornada[]
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

export function CalendarioScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [groupId, setGroupId] = useState('')
  const [panel, setPanel] = useState<Panel | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [equiposSinMatch, setEquiposSinMatch] = useState<string[]>([])
  const solicitud = useRef(0)
  const archivoRef = useRef<HTMLInputElement>(null)
  const writable = canWrite(profile)
  const grupoActual = groupId || panel?.groupId || ''

  function publicar(promesa: Promise<Panel>) {
    const marca = ++solicitud.current
    return promesa.then((siguiente) => {
      if (marca === solicitud.current) {
        setPanel(siguiente)
      }
      return siguiente
    })
  }

  function conGrupo(path: string) {
    if (!grupoActual) {
      return path
    }
    return `${path}${path.includes('?') ? '&' : '?'}groupId=${grupoActual}`
  }

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      setCategoryId('')
      setGroupId('')
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        setCategories(items)
        setCategoryId(items[0]?.id ?? '')
        setGroupId('')
      })
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  useEffect(() => {
    if (!categoryId) {
      setPanel(null)
      return
    }
    const marca = ++solicitud.current
    const query = groupId ? `&groupId=${groupId}` : ''
    api<Panel>(`/api/calendario?categoryId=${categoryId}${query}`)
      .then((siguiente) => {
        if (marca === solicitud.current) {
          setPanel(siguiente)
        }
      })
      .catch((reason: Error) => setError(reason.message))
  }, [categoryId, groupId])

  async function ejecutar(path: string, method: string, body?: unknown) {
    setError(null)
    setEquiposSinMatch([])
    try {
      await publicar(api<Panel>(path, {
        method,
        body: body === undefined ? undefined : JSON.stringify(body),
      }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo completar la operación')
    }
  }

  async function onFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !categoryId || !grupoActual) {
      return
    }
    setError(null)
    setEquiposSinMatch([])
    const body = new FormData()
    body.append('archivo', file)
    try {
      const response = await fetch(`/api/calendario/cargar?categoryId=${categoryId}&groupId=${grupoActual}`, {
        method: 'POST',
        credentials: 'include',
        body,
      })
      const payload = await response.json().catch(() => ({})) as { error?: string; equipos?: string[] } & Partial<Panel>
      if (!response.ok) {
        setEquiposSinMatch(Array.isArray(payload.equipos) ? payload.equipos : [])
        throw new Error(typeof payload.error === 'string' ? payload.error : 'No se pudo cargar el archivo')
      }
      setPanel(payload as Panel)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cargar el archivo')
    }
  }

  const hayCalendario = (panel?.jornadas.length ?? 0) > 0
  const hayGrupos = (panel?.grupos.length ?? 0) > 0
  const puedeCargar = writable && Boolean(grupoActual) && hayGrupos && !hayCalendario

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
      {equiposSinMatch.length > 0 && (
        <ul className="down equipos-sin-match">
          {equiposSinMatch.map((equipo) => <li key={equipo}>{equipo}</li>)}
        </ul>
      )}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select
              value={categoryId}
              onChange={(event) => {
                setCategoryId(event.target.value)
                setGroupId('')
                setEquiposSinMatch([])
                setError(null)
              }}
            >
              {categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </label>
          <label>
            Grupo
            <select
              value={grupoActual}
              onChange={(event) => {
                setGroupId(event.target.value)
                setEquiposSinMatch([])
                setError(null)
              }}
            >
              {(panel?.grupos ?? []).map((item) => (
                <option key={item.id} value={item.id}>{item.nombre}</option>
              ))}
            </select>
          </label>
          {writable && categoryId && (
            <div className="actions">
              <button type="button" onClick={() => ejecutar(conGrupo('/api/calendario/generar'), 'POST', { categoryId })}>
                Generar calendario
              </button>
              <input ref={archivoRef} type="file" accept=".xlsx" hidden onChange={onFile} />
              <button type="button" disabled={!puedeCargar} onClick={() => archivoRef.current?.click()}>
                Cargar calendario predefinido
              </button>
              {hayCalendario && (
                <button
                  type="button"
                  className="ghost"
                  onClick={() => ejecutar(conGrupo(`/api/calendario?categoryId=${categoryId}`), 'DELETE')}
                >
                  Eliminar calendario
                </button>
              )}
            </div>
          )}
        </form>
      )}
      {panel && !hayGrupos && <p>Crea los grupos de la categoría antes de armar el calendario.</p>}
      {panel && hayGrupos && !hayCalendario && (
        <p>No hay calendario para este grupo. Carga el archivo predefinido o genera el calendario de la categoría.</p>
      )}
      {panel && hayCalendario && (
        <div className="calendario">
          {panel.jornadas.map((jornada) => (
            <article key={jornada.id} className="jornada">
              <header>
                <h2>{jornada.nombre}</h2>
                <label>
                  Fecha
                  <FechaCampo
                    value={jornada.fecha}
                    disabled={!writable}
                    ariaLabel={`Fecha de ${jornada.nombre}`}
                    onChange={(fecha) => ejecutar(conGrupo(`/api/calendario/jornada/${jornada.id}`), 'PUT', { fecha })}
                  />
                </label>
              </header>
              <table>
                <thead>
                  <tr>
                    <th>Local</th>
                    <th>Visita</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {jornada.partidos.map((partido) => (
                    <tr key={partido.id}>
                      <td>{partido.textoLocal}</td>
                      <td>{partido.textoVisita}</td>
                      <td>
                        <FechaCampo
                          ariaLabel={`Fecha de ${partido.textoLocal} contra ${partido.textoVisita}`}
                          value={partido.fecha ?? jornada.fecha}
                          disabled={!writable}
                          onChange={(fecha) => ejecutar(conGrupo(`/api/calendario/partido/${partido.id}`), 'PUT', { fecha })}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
