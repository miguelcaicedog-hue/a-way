import { useEffect, useState } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Criterio = {
  codigo: string
  orden: number
  descripcion: string
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

export function DesempateGoleadoresScreen({ profile, editionId }: { profile: Profile; editionId: string }) {
  const [rows, setRows] = useState<Criterio[]>([])
  const [error, setError] = useState<string | null>(null)
  const writable = canWrite(profile)

  useEffect(() => {
    if (!editionId) {
      setRows([])
      return
    }
    api<Criterio[]>(`/api/criterios-desempate-goleador?editionId=${editionId}`)
      .then(setRows)
      .catch((reason: Error) => setError(reason.message))
  }, [editionId])

  async function mover(indice: number, destino: number) {
    if (!writable || destino < 0 || destino >= rows.length) {
      return
    }
    const orden = rows.map((item) => item.codigo)
    const [codigo] = orden.splice(indice, 1)
    orden.splice(destino, 0, codigo)
    setError(null)
    try {
      setRows(await api<Criterio[]>('/api/criterios-desempate-goleador', {
        method: 'PUT',
        body: JSON.stringify({ editionId, codigos: orden }),
      }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cambiar el orden')
    }
  }

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
      {!editionId && <p>Selecciona una edición.</p>}
      {editionId && (
        <ol className="criterios-desempate">
          {rows.map((criterio, indice) => (
            <li key={criterio.codigo}>
              <span>{criterio.descripcion}</span>
              {writable && (
                <span className="actions">
                  <button type="button" className="ghost" disabled={indice === 0} onClick={() => mover(indice, indice - 1)}>Subir</button>
                  <button type="button" className="ghost" disabled={indice === rows.length - 1} onClick={() => mover(indice, indice + 1)}>Bajar</button>
                </span>
              )}
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
