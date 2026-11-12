import { useEffect, useRef, useState } from 'react'
import { api } from './api'
import { FechaCampo } from './Fecha'
import { PartidoModal } from './PartidoModal'

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
  categoryName: string
  jornadaNombre: string
  canchaNombre: string | null
  fecha: string | null
  hora: string | null
  local: string
  visita: string
  golesLocal: number | null
  golesVisita: number | null
  equiposDefinidos: boolean
}

type Panel = {
  hoy: string
  partidos: Partido[]
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

function textoGol(valor: number | null) {
  return valor == null ? '' : String(valor)
}

export function ResultadosScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [ahora, setAhora] = useState(() => new Date())
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel | null>(null)
  const [goles, setGoles] = useState<Record<string, { local: string; visita: string }>>({})
  const [error, setError] = useState<string | null>(null)
  const [partidoDetalle, setPartidoDetalle] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const esperas = useRef<Record<string, number>>({})
  const puedeCualquierFecha = profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
  const esAsistente = profile.memberships.some((item) => item.role === 'ASISTENTE')

  useEffect(() => {
    const relojId = window.setInterval(() => setAhora(new Date()), 1000)
    return () => window.clearInterval(relojId)
  }, [])

  useEffect(() => {
    setCategoryId('')
    setFecha(null)
    setPanel(null)
  }, [editionId])

  useEffect(() => {
    if (!editionId) {
      setCategories([])
      return
    }
    api<Category[]>(`/api/categories?editionId=${editionId}`)
      .then(setCategories)
      .catch((caught: Error) => setError(caught.message))
  }, [editionId])

  useEffect(() => {
    if (!editionId) return
    if (categoryId && !categories.some((item) => item.id === categoryId)) return
    let activo = true
    const params = new URLSearchParams({ editionId })
    if (categoryId) params.set('categoryId', categoryId)
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
    if (!editionId || !fecha) {
      setPanel(null)
      return
    }
    const params = new URLSearchParams({ editionId, fecha })
    if (categoryId) params.set('categoryId', categoryId)
    let activo = true
    api<Panel>(`/api/resultados?${params.toString()}`)
      .then((siguiente) => {
        if (!activo) return
        setPanel(siguiente)
        setGoles((anterior) => {
          const nuevo = { ...anterior }
          for (const partido of siguiente.partidos) {
            if (esperas.current[partido.id]) continue
            nuevo[partido.id] = { local: textoGol(partido.golesLocal), visita: textoGol(partido.golesVisita) }
          }
          return nuevo
        })
        setError(null)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, fecha])

  function puedeEditar(partido: Partido) {
    if (!partido.equiposDefinidos) return false
    if (puedeCualquierFecha) return true
    return esAsistente && partido.fecha != null && panel?.hoy === partido.fecha
  }

  async function mover(anterior: boolean) {
    if (!editionId) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId })
      if (categoryId) params.set('categoryId', categoryId)
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

  function cambiar(partido: Partido, lado: 'local' | 'visita', valor: string) {
    if (valor !== '' && !/^\d+$/.test(valor)) return
    const actual = goles[partido.id] ?? { local: textoGol(partido.golesLocal), visita: textoGol(partido.golesVisita) }
    const siguiente = { ...actual, [lado]: valor }
    setGoles((anterior) => ({ ...anterior, [partido.id]: siguiente }))
    window.clearTimeout(esperas.current[partido.id])
    esperas.current[partido.id] = window.setTimeout(() => {
      delete esperas.current[partido.id]
      guardar(partido.id, siguiente)
    }, 400)
  }

  async function guardar(id: string, marcador: { local: string; visita: string }) {
    setError(null)
    try {
      const guardado = await api<Partido>(`/api/resultados/partido/${id}`, {
        method: 'PUT',
        body: JSON.stringify({
          golesLocal: marcador.local === '' ? null : Number(marcador.local),
          golesVisita: marcador.visita === '' ? null : Number(marcador.visita),
        }),
      })
      if (esperas.current[id]) return
      setGoles((anterior) => ({
        ...anterior,
        [id]: { local: textoGol(guardado.golesLocal), visita: textoGol(guardado.golesVisita) },
      }))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo guardar el marcador')
    }
  }

  const porHora = new Map<string, Partido[]>()
  for (const partido of panel?.partidos ?? []) {
    const hora = partido.hora || 'Sin hora'
    const lista = porHora.get(hora) ?? []
    lista.push(partido)
    porHora.set(hora, lista)
  }
  const grupos = [...porHora.entries()].sort(([a], [b]) => {
    if (a === 'Sin hora') return 1
    if (b === 'Sin hora') return -1
    return a.localeCompare(b)
  })

  return (
    <section className="catalog">
      <p className="reloj">{reloj(ahora)}</p>
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              <option value="">Todas</option>
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
      {editionId && fecha && (panel?.partidos.length ?? 0) === 0 && <p>No hay partidos en esta fecha.</p>}
      {editionId && fecha && grupos.map(([hora, lista]) => (
        <section className="horario-dia" key={hora}>
          <h2>{hora}</h2>
          <table>
            <thead>
              <tr>
                <th>Cancha</th>
                <th>Local</th>
                <th>Goles</th>
                <th>Visita</th>
                <th>Goles</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {lista.map((partido) => {
                const marcador = goles[partido.id] ?? { local: textoGol(partido.golesLocal), visita: textoGol(partido.golesVisita) }
                const edita = puedeEditar(partido)
                return (
                  <tr key={partido.id}>
                    <td>{partido.canchaNombre ?? ''}</td>
                    <td>{partido.local}</td>
                    <td>
                      <input
                        className="marcador"
                        inputMode="numeric"
                        value={marcador.local}
                        disabled={!edita}
                        onChange={(event) => cambiar(partido, 'local', event.target.value)}
                      />
                    </td>
                    <td>{partido.visita}</td>
                    <td>
                      <input
                        className="marcador"
                        inputMode="numeric"
                        value={marcador.visita}
                        disabled={!edita}
                        onChange={(event) => cambiar(partido, 'visita', event.target.value)}
                      />
                    </td>
                    <td>
                      <button type="button" className="silbato" aria-label="Detalle del partido" onClick={() => setPartidoDetalle(partido.id)}>
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path d="M4 10h6l2-2h3a4 4 0 1 1 0 8h-1.2l-1.3 2.2a2 2 0 0 1-3.4 0L8 16H4a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2zm13 1.2a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6z" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </section>
      ))}
      {partidoDetalle && <PartidoModal partidoId={partidoDetalle} onClose={() => setPartidoDetalle(null)} />}
    </section>
  )
}
