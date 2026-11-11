import { useEffect, useRef, useState, type DragEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Category = {
  id: string
  name: string
}

type EquipoGrupo = {
  teamId: string
  nombre: string
}

type Grupo = {
  id: string
  nombre: string
  orden: number
  equipos: EquipoGrupo[]
}

type Panel = {
  categoryId: string
  categoryName: string
  cantidadGrupos: number
  equiposPorGrupo: number | null
  grupos: Grupo[]
  sinGrupo: EquipoGrupo[]
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

export function GruposScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [panel, setPanel] = useState<Panel | null>(null)
  const [sobre, setSobre] = useState<string | null>(null)
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
    api<Panel>(`/api/grupos?categoryId=${categoryId}`)
      .then((siguiente) => {
        if (marca === solicitud.current) {
          setPanel(siguiente)
        }
      })
      .catch((reason: Error) => setError(reason.message))
  }, [categoryId])

  async function ejecutar(path: string, body: unknown) {
    setError(null)
    try {
      await publicar(api<Panel>(path, { method: 'POST', body: JSON.stringify(body) }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo completar la operación')
    }
  }

  async function eliminar(id: string) {
    setError(null)
    try {
      await publicar(api<Panel>(`/api/grupos/${id}`, { method: 'DELETE' }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo eliminar el grupo')
    }
  }

  function arrastrar(event: DragEvent<HTMLElement>, teamId: string) {
    event.dataTransfer.setData('text/plain', teamId)
    event.dataTransfer.effectAllowed = 'move'
  }

  function permitir(event: DragEvent<HTMLElement>, destino: string) {
    if (!writable) {
      return
    }
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
    setSobre(destino)
  }

  async function soltar(event: DragEvent<HTMLElement>, groupId: string | null) {
    event.preventDefault()
    setSobre(null)
    const teamId = event.dataTransfer.getData('text/plain')
    if (!writable || !categoryId || !teamId) {
      return
    }
    setError(null)
    try {
      await publicar(api<Panel>('/api/grupos/equipo', {
        method: 'PUT',
        body: JSON.stringify({ categoryId, teamId, groupId }),
      }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo mover el equipo')
    }
  }

  const cupo = panel?.equiposPorGrupo == null ? 'Todos los equipos' : `Hasta ${panel.equiposPorGrupo} por grupo`

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
              <button type="button" onClick={() => ejecutar('/api/grupos/generar', { categoryId })}>Crear grupos</button>
              <button type="button" onClick={() => ejecutar('/api/grupos', { categoryId })}>Agregar grupo</button>
              <button type="button" onClick={() => ejecutar('/api/grupos/asignar', { categoryId })}>Asignar equipos</button>
            </div>
          )}
        </form>
      )}
      {panel && (
        <>
          <p>{panel.categoryName}: {panel.cantidadGrupos} {panel.cantidadGrupos === 1 ? 'grupo' : 'grupos'}. {cupo}. Arrastra un equipo y suéltalo en un grupo.</p>
          <div className="grupos-tablero">
            <section
              className={sobre === 'libre' ? 'grupo-columna sobre' : 'grupo-columna'}
              onDragOver={(event) => permitir(event, 'libre')}
              onDragLeave={() => setSobre(null)}
              onDrop={(event) => soltar(event, null)}
            >
              <header>
                <h2>Sin grupo</h2>
                <span>{panel.sinGrupo.length}</span>
              </header>
              {panel.sinGrupo.map((equipo) => (
                <article key={equipo.teamId} className="equipo-ficha" role="button" aria-label={equipo.nombre} draggable={writable} onDragStart={(event) => arrastrar(event, equipo.teamId)}>
                  {equipo.nombre}
                </article>
              ))}
              {panel.sinGrupo.length === 0 && <p className="vacio">No hay equipos pendientes.</p>}
            </section>
            {panel.grupos.map((grupo) => (
              <section
                key={grupo.id}
                className={sobre === grupo.id ? 'grupo-columna sobre' : 'grupo-columna'}
                onDragOver={(event) => permitir(event, grupo.id)}
                onDragLeave={() => setSobre(null)}
                onDrop={(event) => soltar(event, grupo.id)}
              >
                <header>
                  <h2>{grupo.nombre}</h2>
                  <span>{grupo.equipos.length}</span>
                  {writable && <button type="button" className="ghost" onClick={() => eliminar(grupo.id)}>Eliminar</button>}
                </header>
                {grupo.equipos.map((equipo) => (
                  <article key={equipo.teamId} className="equipo-ficha" role="button" aria-label={equipo.nombre} draggable={writable} onDragStart={(event) => arrastrar(event, equipo.teamId)}>
                    {equipo.nombre}
                  </article>
                ))}
                {grupo.equipos.length === 0 && <p className="vacio">Suelta equipos aquí.</p>}
              </section>
            ))}
          </div>
        </>
      )}
    </section>
  )
}
