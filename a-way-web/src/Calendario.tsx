import { useEffect, useRef, useState } from 'react'
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
  jornadas: Jornada[]
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

export function CalendarioScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [panel, setPanel] = useState<Panel | null>(null)
  const [error, setError] = useState<string | null>(null)
  const solicitud = useRef(0)
  const writable = canWrite(profile)

  function publicar(promesa: Promise<Panel>) {
    const marca = ++solicitud.current
    return promesa.then((siguiente) => {
      if (marca === solicitud.current) {
        setPanel(siguiente)
      }
      return siguiente
    })
  }

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
    if (!categoryId) {
      setPanel(null)
      return
    }
    const marca = ++solicitud.current
    api<Panel>(`/api/calendario?categoryId=${categoryId}`)
      .then((siguiente) => {
        if (marca === solicitud.current) {
          setPanel(siguiente)
        }
      })
      .catch((reason: Error) => setError(reason.message))
  }, [categoryId])

  async function ejecutar(path: string, method: string, body?: unknown) {
    setError(null)
    try {
      await publicar(api<Panel>(path, {
        method,
        body: body === undefined ? undefined : JSON.stringify(body),
      }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo completar la operación')
    }
  }

  const hayCalendario = (panel?.jornadas.length ?? 0) > 0

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              {categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </label>
          {writable && categoryId && (
            <div className="actions">
              <button type="button" onClick={() => ejecutar('/api/calendario/generar', 'POST', { categoryId })}>Generar calendario</button>
              {hayCalendario && (
                <button type="button" className="ghost" onClick={() => ejecutar(`/api/calendario?categoryId=${categoryId}`, 'DELETE')}>Eliminar calendario</button>
              )}
            </div>
          )}
        </form>
      )}
      {panel && !hayCalendario && <p>No hay calendario. Generar calendario arma los partidos de cada grupo y fase, y con ellos las jornadas.</p>}
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
                    onChange={(fecha) => ejecutar(`/api/calendario/jornada/${jornada.id}`, 'PUT', { fecha })}
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
                          onChange={(fecha) => ejecutar(`/api/calendario/partido/${partido.id}`, 'PUT', { fecha })}
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
