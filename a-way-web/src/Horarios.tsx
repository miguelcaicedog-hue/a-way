import { useEffect, useState } from 'react'
import { api } from './api'
import { FechaCampo } from './Fecha'
import { BotonDescargarImagen } from './DescargarImagen'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Category = {
  id: string
  name: string
}

type Cancha = {
  id: string
  descripcion: string
}

type Partido = {
  id: string
  categoryId: string
  categoryName: string
  jornadaNumero: number
  jornadaNombre: string
  orden: number
  fecha: string | null
  hora: string | null
  canchaId: string | null
  canchaNombre: string | null
  textoLocal: string
  textoVisita: string
  equiposDefinidos: boolean
}

type Panel = {
  canchas: Cancha[]
  jornadas: number[]
  partidos: Partido[]
  aviso: string | null
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

function titulo(iso: string) {
  const texto = new Date(`${iso}T12:00:00`).toLocaleDateString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  })
  return texto.replace(',', '').toLocaleUpperCase('es-EC')
}

function horaVisible(hora: string) {
  return hora.replace(/^0(\d:)/, '$1')
}

function horaEnLaManana(hora: string) {
  const horas = Number(hora.slice(0, 2))
  if (!Number.isInteger(horas) || horas < 0 || horas > 11 || hora.length < 5) {
    return '09:00'
  }
  return hora.slice(0, 5)
}

export function HorariosScreen({ profile, editionId, leagueId }: { profile: Profile; editionId: string; leagueId: string }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel | null>(null)
  const [borrador, setBorrador] = useState<Record<string, { canchaId: string; hora: string }>>({})
  const [horaInicio, setHoraInicio] = useState('09:00')
  const [intervalo, setIntervalo] = useState('40')
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const writable = canWrite(profile)

  useEffect(() => {
    setCategoryId('')
    setFecha(null)
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
    if (!editionId) {
      setPanel(null)
      return
    }
    const params = new URLSearchParams({ editionId })
    if (categoryId) params.set('categoryId', categoryId)
    if (fecha) params.set('fecha', fecha)
    let activo = true
    api<Panel>(`/api/horarios?${params.toString()}`)
      .then((siguiente) => {
        if (activo) {
          setPanel(siguiente)
          setError(null)
        }
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, fecha])

  function aplicar(siguiente: Panel) {
    setPanel(siguiente)
    setBorrador({})
    setError(siguiente.aviso)
  }

  async function guardar(partido: Partido, canchaId: string | null, hora: string | null) {
    setOcupado(true)
    setError(null)
    try {
      const siguiente = await api<Panel>(`/api/horarios/partido/${partido.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          editionId,
          categoryId: categoryId || null,
          fecha,
          canchaId,
          hora,
        }),
      })
      aplicar(siguiente)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo guardar el horario')
    } finally {
      setOcupado(false)
    }
  }

  async function quitarCancha(dia: string, canchaId: string) {
    setOcupado(true)
    setError(null)
    try {
      const siguiente = await api<Panel>('/api/horarios/cancha', {
        method: 'POST',
        body: JSON.stringify({
          editionId,
          categoryId: categoryId || null,
          fecha,
          dia,
          canchaId,
        }),
      })
      aplicar(siguiente)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudieron quitar los partidos de la cancha')
    } finally {
      setOcupado(false)
    }
  }

  async function filtrarProximaFecha() {
    if (!editionId) return
    setOcupado(true)
    setError(null)
    try {
      const params = new URLSearchParams({ editionId })
      if (categoryId) params.set('categoryId', categoryId)
      if (fecha) params.set('desde', fecha)
      const respuesta = await api<{ fecha: string }>(`/api/horarios/proxima-fecha?${params.toString()}`)
      setFecha(respuesta.fecha)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No hay otra fecha sin horario y cancha')
    } finally {
      setOcupado(false)
    }
  }

  async function asignarDia() {
    if (!fecha) return
    setOcupado(true)
    setError(null)
    try {
      const siguiente = await api<Panel>('/api/horarios/dia', {
        method: 'POST',
        body: JSON.stringify({
          editionId,
          categoryId: categoryId || null,
          fecha,
          horaInicio,
          intervaloMinutos: Number(intervalo),
        }),
      })
      aplicar(siguiente)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudieron asignar los horarios')
    } finally {
      setOcupado(false)
    }
  }

  const asignados = (panel?.partidos ?? []).filter((partido) => partido.fecha && partido.hora && partido.canchaId)
  const pendientes = (panel?.partidos ?? []).filter((partido) => !partido.hora || !partido.canchaId)
  const porFecha = new Map<string, Partido[]>()
  for (const partido of asignados) {
    const lista = porFecha.get(partido.fecha as string) ?? []
    lista.push(partido)
    porFecha.set(partido.fecha as string, lista)
  }
  const dias = [...porFecha.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([dia, lista]) => {
      const grupos = new Map<string, { id: string; nombre: string; partidos: Partido[] }>()
      for (const cancha of panel?.canchas ?? []) {
        grupos.set(cancha.id, { id: cancha.id, nombre: cancha.descripcion, partidos: [] })
      }
      for (const partido of lista) {
        const grupo = grupos.get(partido.canchaId as string) ?? {
          id: partido.canchaId as string,
          nombre: partido.canchaNombre ?? 'Cancha',
          partidos: [],
        }
        grupo.partidos.push(partido)
        grupos.set(partido.canchaId as string, grupo)
      }
      return {
        dia,
        canchas: [...grupos.values()].map((grupo) => ({
          ...grupo,
          partidos: grupo.partidos.sort((a, b) => (a.hora ?? '').localeCompare(b.hora ?? '') || a.orden - b.orden),
        })),
      }
    })

  return (
    <section className="catalog">
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
          <button type="button" disabled={ocupado} onClick={filtrarProximaFecha}>FILTRAR PROXIMA FECHA</button>
          <BotonDescargarImagen
            leagueId={leagueId}
            tipo="HORARIOS"
            fecha={dias.length === 1 ? dias[0].dia : fecha}
            horarios={asignados.map((partido) => ({
              canchaNombre: partido.canchaNombre,
              hora: partido.hora,
              textoLocal: partido.textoLocal,
              textoVisita: partido.textoVisita,
            }))}
          />
        </form>
      )}
      {editionId && writable && (
        <form className="horario-asignar" onSubmit={(event) => event.preventDefault()}>
          <div>
            <h2>Asignación del día</h2>
            <p>El primer partido sale a la hora de inicio. Los siguientes, cada cierto número de minutos. Un equipo no juega el turno que sigue. Las canchas quedan, en lo posible, con la misma cantidad de partidos.</p>
          </div>
          <label>
            Hora de inicio
            <input type="time" step={60} value={horaInicio} onChange={(event) => setHoraInicio(event.target.value)} />
          </label>
          <label>
            Cada
            <span className="horario-intervalo">
              <input
                type="number"
                min={5}
                max={180}
                step={5}
                value={intervalo}
                onChange={(event) => setIntervalo(event.target.value)}
              />
              <span>minutos</span>
            </span>
          </label>
          <button
            type="button"
            disabled={!fecha || ocupado || !horaInicio || !Number.isInteger(Number(intervalo)) || Number(intervalo) < 5 || Number(intervalo) > 180}
            onClick={asignarDia}
          >
            Asignar horarios del día
          </button>
        </form>
      )}
      {editionId && panel && panel.canchas.length === 0 && (
        <p>No hay canchas activas. Créalas en Canchas.</p>
      )}
      {dias.map((dia) => (
        <article className="horario-dia" key={dia.dia}>
          <h2>{titulo(dia.dia)}</h2>
          <table>
            <thead>
              <tr>
                <th>Cancha</th>
                <th>Hora</th>
                <th className="numero">Nº</th>
                <th>Local</th>
                <th>Visita</th>
                {!categoryId && <th>Categoría</th>}
                {writable && <th></th>}
              </tr>
            </thead>
            <tbody>
              {dia.canchas.map((cancha) => (
                cancha.partidos.length === 0 ? (
                  <tr key={cancha.id}>
                    <th className="horario-cancha">{cancha.nombre}</th>
                    <td colSpan={4 + (categoryId ? 0 : 1) + (writable ? 1 : 0)}>Sin partidos</td>
                  </tr>
                ) : cancha.partidos.map((partido, indice) => (
                  <tr key={partido.id}>
                    {indice === 0 && (
                      <th className="horario-cancha" rowSpan={cancha.partidos.length}>
                        {cancha.nombre}
                        {writable && (
                          <button type="button" className="ghost" disabled={ocupado} onClick={() => quitarCancha(dia.dia, cancha.id)}>
                            ELIMINAR PARTIDOS
                          </button>
                        )}
                      </th>
                    )}
                    <td>{horaVisible(partido.hora as string)}</td>
                    <td className="numero">{indice + 1}</td>
                    <td>{partido.textoLocal}</td>
                    <td>{partido.textoVisita}</td>
                    {!categoryId && <td>{partido.categoryName}</td>}
                    {writable && (
                      <td>
                        <button type="button" className="ghost" disabled={ocupado} onClick={() => guardar(partido, null, null)}>
                          Quitar
                        </button>
                      </td>
                    )}
                  </tr>
                ))
              ))}
            </tbody>
          </table>
        </article>
      ))}
      {editionId && panel && (
        <>
          <h2>Sin horario</h2>
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Jornada</th>
                {!categoryId && <th>Categoría</th>}
                <th>Local</th>
                <th>Visita</th>
                {writable && <th>Cancha</th>}
                {writable && <th>Hora</th>}
                {writable && <th></th>}
              </tr>
            </thead>
            <tbody>
              {pendientes.map((partido) => {
                const actual = borrador[partido.id] ?? { canchaId: '', hora: '' }
                const hora = actual.hora || horaEnLaManana(horaInicio)
                return (
                  <tr key={partido.id}>
                    <td>{partido.fecha ? titulo(partido.fecha).split(' ').slice(1).join(' ') : 'Sin fecha'}</td>
                    <td>{partido.jornadaNumero}</td>
                    {!categoryId && <td>{partido.categoryName}</td>}
                    <td>{partido.textoLocal}</td>
                    <td>{partido.textoVisita}</td>
                    {writable && (
                      <>
                        <td>
                          <select
                            value={actual.canchaId}
                            onChange={(event) => setBorrador({ ...borrador, [partido.id]: { ...actual, canchaId: event.target.value } })}
                          >
                            <option value="">Cancha</option>
                            {(panel.canchas).map((cancha) => (
                              <option key={cancha.id} value={cancha.id}>{cancha.descripcion}</option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <input
                            type="time"
                            step={60}
                            value={hora}
                            onChange={(event) => setBorrador({ ...borrador, [partido.id]: { ...actual, hora: event.target.value || horaEnLaManana(horaInicio) } })}
                          />
                        </td>
                        <td>
                          <button
                            type="button"
                            disabled={ocupado || !actual.canchaId || !hora}
                            onClick={() => guardar(partido, actual.canchaId, hora)}
                          >
                            Guardar
                          </button>
                        </td>
                      </>
                    )}
                  </tr>
                )
              })}
              {pendientes.length === 0 && (
                <tr>
                  <td colSpan={writable ? (categoryId ? 7 : 8) : (categoryId ? 4 : 5)}>No hay partidos sin horario.</td>
                </tr>
              )}
            </tbody>
          </table>
        </>
      )}
    </section>
  )
}
