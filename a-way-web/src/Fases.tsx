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

type GrupoFase = {
  id: string
  nombre: string
}

type Posicion = {
  groupId: string
  posicion: number
  etiqueta: string
  ocupada: boolean
}

type Cruce = {
  id: string
  orden: number
  editable: boolean
  groupIdLocal: string | null
  posicionLocal: number | null
  textoLocal: string | null
  groupIdVisita: string | null
  posicionVisita: number | null
  textoVisita: string | null
}

type Fase = {
  id: string
  nombre: string
  orden: number
  modalidad: 'IDA' | 'IDA_Y_VUELTA'
  cruces: Cruce[]
}

type Panel = {
  categoryId: string
  categoryName: string
  cantidadFases: number
  grupos: GrupoFase[]
  posiciones: Posicion[]
  fases: Fase[]
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

export function FasesScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
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
    api<Panel>(`/api/fases?categoryId=${categoryId}`)
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

  function arrastrar(event: DragEvent<HTMLElement>, groupId: string, posicion: number, cruceId?: string, lado?: string) {
    const origen = cruceId && lado ? `|${cruceId}|${lado}` : ''
    event.dataTransfer.setData('text/plain', `${groupId}|${posicion}${origen}`)
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

  async function soltarCruce(event: DragEvent<HTMLElement>, cruceId: string, lado: 'LOCAL' | 'VISITA') {
    event.preventDefault()
    setSobre(null)
    const datos = event.dataTransfer.getData('text/plain').split('|')
    const groupId = datos[0]
    const posicion = Number(datos[1])
    if (!writable || !groupId || !Number.isInteger(posicion)) {
      return
    }
    await ejecutar('/api/fases/cruce', 'PUT', { cruceId, lado, groupId, posicion })
  }

  async function soltarLibre(event: DragEvent<HTMLElement>) {
    event.preventDefault()
    setSobre(null)
    const datos = event.dataTransfer.getData('text/plain').split('|')
    const cruceId = datos[2]
    const lado = datos[3]
    if (!writable || !cruceId || (lado !== 'LOCAL' && lado !== 'VISITA')) {
      return
    }
    await ejecutar('/api/fases/cruce', 'PUT', { cruceId, lado, groupId: null, posicion: null })
  }

  const hayFases = (panel?.fases.length ?? 0) > 0

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
              <button type="button" onClick={() => ejecutar('/api/fases/generar', 'POST', { categoryId })}>Crear fases</button>
              {hayFases && (
                <button type="button" className="ghost" onClick={() => ejecutar(`/api/fases?categoryId=${categoryId}`, 'DELETE')}>Eliminar fases</button>
              )}
            </div>
          )}
        </form>
      )}
      {panel && (
        <>
          <p>
            {panel.categoryName}: {panel.cantidadFases} {panel.cantidadFases === 1 ? 'fase' : 'fases'}.
            {panel.grupos.length === 0
              ? ' Esta categoría no tiene grupos.'
              : panel.cantidadFases === 1
                ? ' El campeón sale del orden de la tabla acumulada.'
                : hayFases
                  ? ' Arrastra cada posición del grupo a un cruce de la fase. Un lado vacío pasa directo.'
                  : ' Crear fases arma las rondas. Después arrastra cada posición a un cruce.'}
          </p>
          <div className="fases-tablero">
            <section
              className={sobre === 'libre' ? 'fase-columna sobre' : 'fase-columna'}
              onDragOver={(event) => permitir(event, 'libre')}
              onDragLeave={() => setSobre(null)}
              onDrop={soltarLibre}
            >
              <header>
                <h2>Grupos</h2>
                <span>{panel.grupos.length}</span>
              </header>
              {panel.grupos.map((grupo) => {
                const posiciones = panel.posiciones.filter((item) => item.groupId === grupo.id)
                return (
                  <div key={grupo.id} className="fase-grupo">
                    <h3>{grupo.nombre}</h3>
                    {posiciones.map((item) => (
                      <article
                        key={`${item.groupId}-${item.posicion}`}
                        className={item.ocupada ? 'equipo-ficha ocupada' : 'equipo-ficha'}
                        role="button"
                        aria-label={item.etiqueta}
                        draggable={writable && hayFases}
                        onDragStart={(event) => arrastrar(event, item.groupId, item.posicion)}
                      >
                        {item.etiqueta}
                      </article>
                    ))}
                    {posiciones.length === 0 && <p className="vacio">Sin equipos.</p>}
                  </div>
                )
              })}
              {panel.grupos.length === 0 && <p className="vacio">Crea los grupos de la categoría.</p>}
            </section>
            {panel.fases.map((fase) => (
              <section key={fase.id} className="fase-columna">
                <header>
                  <h2>{fase.nombre}</h2>
                </header>
                <label>
                  Modalidad
                  <select
                    value={fase.modalidad}
                    disabled={!writable}
                    onChange={(event) => ejecutar(`/api/fases/${fase.id}/modalidad`, 'PUT', { modalidad: event.target.value })}
                  >
                    <option value="IDA">Ida</option>
                    <option value="IDA_Y_VUELTA">Ida y vuelta</option>
                  </select>
                </label>
                {fase.orden === 1 && fase.cruces.length === 0 && (
                  <p className="vacio">
                    {panel.cantidadFases === 1
                      ? 'El campeón sale del orden de la tabla acumulada.'
                      : 'La tabla acumulada define quién clasifica.'}
                  </p>
                )}
                {fase.cruces.map((cruce) => (
                  <article key={cruce.id} className="cruce" aria-label={`${fase.nombre} ${cruce.orden}`}>
                    <Lado
                      texto={cruce.textoLocal}
                      editable={writable && cruce.editable}
                      activo={sobre === `${cruce.id}-LOCAL`}
                      onDragOver={(event) => cruce.editable && permitir(event, `${cruce.id}-LOCAL`)}
                      onDrop={(event) => soltarCruce(event, cruce.id, 'LOCAL')}
                      onDragStart={cruce.groupIdLocal && cruce.posicionLocal
                        ? (event) => arrastrar(event, cruce.groupIdLocal as string, cruce.posicionLocal as number, cruce.id, 'LOCAL')
                        : undefined}
                    />
                    <Lado
                      texto={cruce.textoVisita}
                      editable={writable && cruce.editable}
                      activo={sobre === `${cruce.id}-VISITA`}
                      onDragOver={(event) => cruce.editable && permitir(event, `${cruce.id}-VISITA`)}
                      onDrop={(event) => soltarCruce(event, cruce.id, 'VISITA')}
                      onDragStart={cruce.groupIdVisita && cruce.posicionVisita
                        ? (event) => arrastrar(event, cruce.groupIdVisita as string, cruce.posicionVisita as number, cruce.id, 'VISITA')
                        : undefined}
                    />
                  </article>
                ))}
              </section>
            ))}
          </div>
        </>
      )}
    </section>
  )
}

function Lado({ texto, editable, activo, onDragOver, onDrop, onDragStart }: {
  texto: string | null
  editable: boolean
  activo: boolean
  onDragOver: (event: DragEvent<HTMLElement>) => void
  onDrop: (event: DragEvent<HTMLElement>) => void
  onDragStart?: (event: DragEvent<HTMLElement>) => void
}) {
  const clase = activo ? 'cruce-lado sobre' : 'cruce-lado'
  if (editable && texto && onDragStart) {
    return (
      <div className={clase} onDragOver={onDragOver} onDrop={onDrop}>
        <article className="equipo-ficha" role="button" aria-label={texto} draggable onDragStart={onDragStart}>{texto}</article>
      </div>
    )
  }
  return (
    <div className={clase} onDragOver={editable ? onDragOver : undefined} onDrop={editable ? onDrop : undefined}>
      <span>{texto ?? (editable ? 'Suelta una posición' : '—')}</span>
    </div>
  )
}
