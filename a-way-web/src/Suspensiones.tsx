import { useEffect, useState, type FormEvent } from 'react'
import { api } from './api'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Regla = {
  leagueId: string
  momento: string
  rojaJornadas: number
  amarillasAcumuladas: number
  amarillasJornadas: number
  amarillasMomento: string
  rojasAcumuladas: number
  rojasJornadas: number
  rojasMomento: string
  faltaMediaJornadas: number
  faltaGrave: string
  estadoRoja: string
  estadoAmarillas: string
  estadoRojas: string
  estadoFaltaMedia: string
  estadoFaltaGrave: string
}

function Estado({ valor, writable, onChange }: { valor: string; writable: boolean; onChange: (valor: string) => void }) {
  return (
    <label>
      Estado
      <select value={valor} disabled={!writable} onChange={(event) => onChange(event.target.value)}>
        <option value="ACTIVO">Activo</option>
        <option value="INACTIVO">Inactivo</option>
      </select>
    </label>
  )
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

export function SuspensionesScreen({ profile, leagueId }: { profile: Profile; leagueId: string }) {
  const [momento, setMomento] = useState('NEXT_MATCHDAY')
  const [rojaJornadas, setRojaJornadas] = useState('1')
  const [amarillasAcumuladas, setAmarillasAcumuladas] = useState('3')
  const [amarillasJornadas, setAmarillasJornadas] = useState('1')
  const [amarillasMomento, setAmarillasMomento] = useState('NEXT_MATCHDAY')
  const [rojasAcumuladas, setRojasAcumuladas] = useState('2')
  const [rojasJornadas, setRojasJornadas] = useState('2')
  const [rojasMomento, setRojasMomento] = useState('NEXT_MATCHDAY')
  const [faltaMediaJornadas, setFaltaMediaJornadas] = useState('3')
  const [estadoRoja, setEstadoRoja] = useState('ACTIVO')
  const [estadoAmarillas, setEstadoAmarillas] = useState('ACTIVO')
  const [estadoRojas, setEstadoRojas] = useState('ACTIVO')
  const [estadoFaltaMedia, setEstadoFaltaMedia] = useState('ACTIVO')
  const [estadoFaltaGrave, setEstadoFaltaGrave] = useState('ACTIVO')
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const writable = canWrite(profile)

  useEffect(() => {
    if (!leagueId) return
    let activo = true
    setError(null)
    setAviso(null)
    api<Regla>(`/api/suspensiones?leagueId=${leagueId}`)
      .then((regla) => {
        if (!activo) return
        setMomento(regla.momento)
        setRojaJornadas(String(regla.rojaJornadas))
        setAmarillasAcumuladas(String(regla.amarillasAcumuladas))
        setAmarillasJornadas(String(regla.amarillasJornadas))
        setAmarillasMomento(regla.amarillasMomento)
        setRojasAcumuladas(String(regla.rojasAcumuladas))
        setRojasJornadas(String(regla.rojasJornadas))
        setRojasMomento(regla.rojasMomento)
        setFaltaMediaJornadas(String(regla.faltaMediaJornadas))
        setEstadoRoja(regla.estadoRoja)
        setEstadoAmarillas(regla.estadoAmarillas)
        setEstadoRojas(regla.estadoRojas)
        setEstadoFaltaMedia(regla.estadoFaltaMedia)
        setEstadoFaltaGrave(regla.estadoFaltaGrave)
      })
      .catch((reason: Error) => {
        if (activo) setError(reason.message)
      })
    return () => {
      activo = false
    }
  }, [leagueId])

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setAviso(null)
    try {
      const regla = await api<Regla>('/api/suspensiones', {
        method: 'PUT',
        body: JSON.stringify({
          leagueId,
          momento,
          rojaJornadas: Number(rojaJornadas),
          amarillasAcumuladas: Number(amarillasAcumuladas),
          amarillasJornadas: Number(amarillasJornadas),
          amarillasMomento,
          rojasAcumuladas: Number(rojasAcumuladas),
          rojasJornadas: Number(rojasJornadas),
          rojasMomento,
          faltaMediaJornadas: Number(faltaMediaJornadas),
          estadoRoja,
          estadoAmarillas,
          estadoRojas,
          estadoFaltaMedia,
          estadoFaltaGrave,
        }),
      })
      setMomento(regla.momento)
      setAmarillasMomento(regla.amarillasMomento)
      setRojasMomento(regla.rojasMomento)
      setEstadoRoja(regla.estadoRoja)
      setEstadoAmarillas(regla.estadoAmarillas)
      setEstadoRojas(regla.estadoRojas)
      setEstadoFaltaMedia(regla.estadoFaltaMedia)
      setEstadoFaltaGrave(regla.estadoFaltaGrave)
      setAviso('Reglas guardadas.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudieron guardar las reglas')
    }
  }

  return (
    <section className="catalog">
      {error && <p className="down">{error}</p>}
      {aviso && <p>{aviso}</p>}
      {!leagueId && <p>Selecciona una liga.</p>}
      {leagueId && (
        <form onSubmit={guardar}>
          <h2>Tarjeta roja</h2>
          <Estado valor={estadoRoja} writable={writable} onChange={setEstadoRoja} />
          <label>
            Jornadas de suspensión
            <input type="number" min={0} value={rojaJornadas} disabled={!writable} onChange={(event) => setRojaJornadas(event.target.value)} required />
          </label>
          <label>
            Empieza en
            <select value={momento} disabled={!writable} onChange={(event) => setMomento(event.target.value)}>
              <option value="NEXT_MATCHDAY">Jornada siguiente</option>
              <option value="FIRST_MATCHDAY_OF_DAY">Primera jornada de la siguiente fecha</option>
            </select>
          </label>
          <h2>Acumulación de amarillas</h2>
          <Estado valor={estadoAmarillas} writable={writable} onChange={setEstadoAmarillas} />
          <label>
            Tarjetas acumuladas
            <input type="number" min={1} value={amarillasAcumuladas} disabled={!writable} onChange={(event) => setAmarillasAcumuladas(event.target.value)} required />
          </label>
          <label>
            Jornadas de suspensión
            <input type="number" min={0} value={amarillasJornadas} disabled={!writable} onChange={(event) => setAmarillasJornadas(event.target.value)} required />
          </label>
          <label>
            Empieza en
            <select value={amarillasMomento} disabled={!writable} onChange={(event) => setAmarillasMomento(event.target.value)}>
              <option value="NEXT_MATCHDAY">Jornada siguiente</option>
              <option value="FIRST_MATCHDAY_OF_DAY">Primera jornada de la siguiente fecha</option>
            </select>
          </label>
          <h2>Acumulación de rojas</h2>
          <Estado valor={estadoRojas} writable={writable} onChange={setEstadoRojas} />
          <label>
            Tarjetas acumuladas
            <input type="number" min={1} value={rojasAcumuladas} disabled={!writable} onChange={(event) => setRojasAcumuladas(event.target.value)} required />
          </label>
          <label>
            Jornadas de suspensión
            <input type="number" min={0} value={rojasJornadas} disabled={!writable} onChange={(event) => setRojasJornadas(event.target.value)} required />
          </label>
          <label>
            Empieza en
            <select value={rojasMomento} disabled={!writable} onChange={(event) => setRojasMomento(event.target.value)}>
              <option value="NEXT_MATCHDAY">Jornada siguiente</option>
              <option value="FIRST_MATCHDAY_OF_DAY">Primera jornada de la siguiente fecha</option>
            </select>
          </label>
          <h2>Falta media</h2>
          <Estado valor={estadoFaltaMedia} writable={writable} onChange={setEstadoFaltaMedia} />
          <label>
            Jornadas de suspensión
            <input type="number" min={0} value={faltaMediaJornadas} disabled={!writable} onChange={(event) => setFaltaMediaJornadas(event.target.value)} required />
          </label>
          <h2>Falta grave</h2>
          <Estado valor={estadoFaltaGrave} writable={writable} onChange={setEstadoFaltaGrave} />
          <p>Eliminación del campeonato.</p>
          {writable && <button type="submit">Guardar</button>}
        </form>
      )}
    </section>
  )
}
