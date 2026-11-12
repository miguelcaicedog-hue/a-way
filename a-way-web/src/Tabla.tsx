import { useEffect, useState } from 'react'
import { api } from './api'
import { FechaCampo } from './Fecha'

type Category = {
  id: string
  name: string
}

type Fila = {
  posicion: number
  teamId: string
  equipo: string
  pj: number
  ganados: number
  empatados: number
  perdidos: number
  puntos: number
  golesFavor: number
  golesContra: number
  diferencia: number
}

type GrupoTabla = {
  id: string | null
  nombre: string
  equipos: Fila[]
}

type Tabla = {
  fecha: string
  grupos: GrupoTabla[]
}

function reloj(ahora: Date) {
  return ahora.toLocaleString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

export function TablaScreen({ editionId }: { editionId: string }) {
  const [ahora, setAhora] = useState(() => new Date())
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [tabla, setTabla] = useState<Tabla | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  useEffect(() => {
    const relojId = window.setInterval(() => setAhora(new Date()), 1000)
    return () => window.clearInterval(relojId)
  }, [])

  useEffect(() => {
    setCategories([])
    setCategoryId('')
    setFecha(null)
    setTabla(null)
  }, [editionId])

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      return
    }
    let activo = true
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        if (!activo) return
        setCategories(items)
        setCategoryId(items[0]?.id ?? '')
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId])

  useEffect(() => {
    if (!editionId || !categoryId) return
    if (!categories.some((item) => item.id === categoryId)) return
    let activo = true
    const params = new URLSearchParams({ editionId, categoryId })
    api<{ fecha: string | null }>(`/api/resultados/fecha-cercana?${params.toString()}`)
      .then((respuesta) => {
        if (activo) setFecha(respuesta.fecha)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, categories])

  useEffect(() => {
    if (!editionId || !categoryId || !fecha) {
      setTabla(null)
      return
    }
    const params = new URLSearchParams({ editionId, categoryId, fecha })
    let activo = true
    function cargar() {
      api<Tabla>(`/api/tabla?${params.toString()}`)
        .then((siguiente) => {
          if (!activo) return
          setTabla(siguiente)
          setError(null)
        })
        .catch((caught: Error) => {
          if (activo) setError(caught.message)
        })
    }
    cargar()
    function alVolver() {
      if (document.visibilityState === 'visible') cargar()
    }
    document.addEventListener('visibilitychange', alVolver)
    return () => {
      activo = false
      document.removeEventListener('visibilitychange', alVolver)
    }
  }, [editionId, categoryId, fecha])

  async function mover(anterior: boolean) {
    if (!editionId || !categoryId) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId, categoryId })
      if (fecha) params.set('desde', fecha)
      const ruta = anterior ? 'fecha-anterior' : 'proxima-fecha'
      const respuesta = await api<{ fecha: string }>(`/api/resultados/${ruta}?${params.toString()}`)
      setFecha(respuesta.fecha)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No hay otra fecha de partidos')
    } finally {
      setOcupado(false)
    }
  }

  const grupos = tabla?.grupos ?? []
  const variosGrupos = grupos.length > 1

  return (
    <section className="catalog">
      <p className="reloj">{reloj(ahora)}</p>
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && categories.length === 0 && <p>Esta edición no tiene categorías.</p>}
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
          <button type="button" disabled={ocupado} onClick={() => mover(true)}>FILTRAR FECHA ANTERIOR</button>
          <button type="button" disabled={ocupado} onClick={() => mover(false)}>FILTRAR PROXIMA FECHA</button>
        </form>
      )}
      {editionId && categoryId && !fecha && <p>No hay fechas de partidos en esta categoría.</p>}
      {editionId && fecha && tabla && grupos.length === 0 && <p>No hay equipos en esta categoría.</p>}
      {grupos.map((grupo) => (
        <section className="tabla-posiciones" key={grupo.id ?? 'unica'}>
          {variosGrupos && <h2>{grupo.nombre}</h2>}
          <table>
            <thead>
              <tr>
                <th className="pos"></th>
                <th>Equipo</th>
                <th>PJ</th>
                <th>G</th>
                <th>E</th>
                <th>P</th>
                <th className="pts">Pts</th>
                <th>GF</th>
                <th>GC</th>
                <th>Dif</th>
              </tr>
            </thead>
            <tbody>
              {grupo.equipos.map((fila) => (
                <tr key={fila.teamId}>
                  <td className="pos">{fila.posicion}</td>
                  <td className="equipo">{fila.equipo}</td>
                  <td>{fila.pj}</td>
                  <td>{fila.ganados}</td>
                  <td>{fila.empatados}</td>
                  <td>{fila.perdidos}</td>
                  <td className="pts">{fila.puntos}</td>
                  <td>{fila.golesFavor}</td>
                  <td>{fila.golesContra}</td>
                  <td>{fila.diferencia}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}
    </section>
  )
}
