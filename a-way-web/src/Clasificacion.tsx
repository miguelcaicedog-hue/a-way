import { useEffect, useState } from 'react'
import { api } from './api'
import { aDia, FechaCampo } from './Fecha'

type Category = { id: string; name: string }

type Llave = {
  id: string
  local: string | null
  visita: string | null
  localEquipo: boolean
  visitaEquipo: boolean
  golesLocal: number | null
  golesVisita: number | null
}

type Ronda = {
  nombre: string
  orden: number
  lado: 'IZQUIERDA' | 'DERECHA' | 'CENTRO'
  llaves: Llave[]
}

type Cuadro = {
  fecha: string
  rondas: Ronda[]
}

function claseEquipo(nombre: string | null, equipo: boolean, gana: boolean) {
  if (!nombre) return 'cuadro-equipo vacio'
  if (!equipo) return 'cuadro-equipo puesto'
  return gana ? 'cuadro-equipo avanza' : 'cuadro-equipo'
}

function Casillero({ nombre, equipo, goles, gana }: { nombre: string | null; equipo: boolean; goles: number | null; gana: boolean }) {
  return (
    <div className={claseEquipo(nombre, equipo, gana)}>
      <span>{nombre || '\u00a0'}</span>
      {goles != null && <span className="goles">{goles}</span>}
    </div>
  )
}

function Tarjeta({ llave }: { llave: Llave }) {
  const hayMarcador = llave.golesLocal != null && llave.golesVisita != null
  const localGana = hayMarcador && llave.golesLocal! > llave.golesVisita!
  const visitaGana = hayMarcador && llave.golesVisita! > llave.golesLocal!
  return (
    <article className="cuadro-llave">
      <Casillero nombre={llave.local} equipo={llave.localEquipo} goles={llave.golesLocal} gana={localGana} />
      <Casillero nombre={llave.visita} equipo={llave.visitaEquipo} goles={llave.golesVisita} gana={visitaGana} />
    </article>
  )
}

function Columna({ ronda }: { ronda: Ronda }) {
  const parejas: Llave[][] = []
  for (let indice = 0; indice < ronda.llaves.length; indice += 2) {
    parejas.push(ronda.llaves.slice(indice, indice + 2))
  }
  return (
    <section className="cuadro-ronda">
      <h2>{ronda.nombre}</h2>
      <div className="cuadro-llaves">
        {parejas.map((par) => (
          <div className={par.length === 1 ? 'cuadro-pareja sola' : 'cuadro-pareja'} key={par.map((llave) => llave.id).join('-')}>
            {par.map((llave) => (
              <Tarjeta key={llave.id} llave={llave} />
            ))}
          </div>
        ))}
      </div>
    </section>
  )
}

export function ClasificacionScreen({
  editionId,
  leagueId,
  leagueName,
  hasLogo,
  editionName,
  logoVersion,
}: {
  editionId: string
  leagueId: string
  leagueName: string
  hasLogo: boolean
  editionName: string
  logoVersion: number
}) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoriasListas, setCategoriasListas] = useState(false)
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [cuadro, setCuadro] = useState<Cuadro | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCategories([])
    setCategoriasListas(false)
    setCategoryId('')
    setFecha(null)
    setCuadro(null)
  }, [editionId])

  useEffect(() => {
    if (!editionId) return
    let activo = true
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        if (!activo) return
        setCategories(items)
        setCategoryId(items[0]?.id ?? '')
        setCategoriasListas(true)
      })
      .catch((caught: Error) => {
        if (!activo) return
        setError(caught.message)
        setCategoriasListas(true)
      })
    return () => {
      activo = false
    }
  }, [editionId])

  useEffect(() => {
    if (!editionId || !categoryId) {
      setCuadro(null)
      return
    }
    if (!categories.some((item) => item.id === categoryId)) return
    const params = new URLSearchParams({ editionId, categoryId })
    if (fecha) params.set('fecha', fecha)
    let activo = true
    api<Cuadro>(`/api/clasificacion?${params.toString()}`)
      .then((siguiente) => {
        if (!activo) return
        setCuadro(siguiente)
        setError(null)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, fecha, categories])

  const rondas = cuadro?.rondas ?? []
  const izquierda = rondas.filter((ronda) => ronda.lado === 'IZQUIERDA').sort((a, b) => a.orden - b.orden)
  const derecha = rondas.filter((ronda) => ronda.lado === 'DERECHA').sort((a, b) => b.orden - a.orden)
  const centro = rondas.filter((ronda) => ronda.lado === 'CENTRO').sort((a, b) => a.orden - b.orden)

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && categoriasListas && categories.length === 0 && <p>Esta edición no tiene categorías.</p>}
      {editionId && categories.length > 0 && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)} required>
              {categories.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </label>
          <label>
            Fecha
            <FechaCampo value={fecha} onChange={setFecha} ariaLabel="Fecha" />
          </label>
        </form>
      )}
      {cuadro && rondas.length === 0 && <p>Esta categoría no tiene cruces de eliminación.</p>}
      {cuadro && rondas.length > 0 && (
        <>
          <p className="cuadro-fecha">Cuadro al {aDia(cuadro.fecha)}</p>
          <div className="cuadro">
            {izquierda.length > 0 && (
              <div className="cuadro-lado izquierda">
                {izquierda.map((ronda) => (
                  <Columna key={`${ronda.orden}-izq`} ronda={ronda} />
                ))}
              </div>
            )}
            <div className="cuadro-centro">
              {centro.map((ronda, indice) => (
                <section className="cuadro-ronda" key={`${ronda.orden}-centro`}>
                  <h2>{ronda.nombre}</h2>
                  <div className="cuadro-llaves">
                    {ronda.llaves.map((llave) => (
                      <div className="cuadro-final" key={llave.id}>
                        <Tarjeta llave={llave} />
                        {indice === centro.length - 1 && (
                          <div className="cuadro-liga">
                            {hasLogo && leagueId && (
                              <img className="cuadro-escudo" alt="" src={`/api/leagues/${leagueId}/logo?v=${logoVersion}`} />
                            )}
                            {leagueName && <p className="cuadro-liga-nombre">{leagueName}</p>}
                            {editionName && <p className="cuadro-liga-edicion">{editionName}</p>}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </section>
              ))}
            </div>
            {derecha.length > 0 && (
              <div className="cuadro-lado derecha">
                {derecha.map((ronda) => (
                  <Columna key={`${ronda.orden}-der`} ronda={ronda} />
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </section>
  )
}
