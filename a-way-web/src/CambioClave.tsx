import { useEffect, useState, type FormEvent } from 'react'
import { api } from './api'

type Politica = {
  longitudMinima: number
  requiereLetra: boolean
  requiereNumero: boolean
  requiereEspecial: boolean
}

function faltantes(clave: string, politica: Politica | null) {
  if (!politica) return []
  const pendientes: string[] = []
  if (clave.length < politica.longitudMinima) {
    pendientes.push(`Debe tener al menos ${politica.longitudMinima} caracteres.`)
  }
  if (politica.requiereLetra && !/\p{L}/u.test(clave)) {
    pendientes.push('Debe incluir al menos una letra.')
  }
  if (politica.requiereNumero && !/\p{N}/u.test(clave)) {
    pendientes.push('Debe incluir al menos un número.')
  }
  if (politica.requiereEspecial && ![...clave].some((caracter) => !/\p{L}/u.test(caracter) && !/\p{N}/u.test(caracter))) {
    pendientes.push('Debe incluir al menos un caracter especial.')
  }
  return pendientes
}

function CampoClave({
  etiqueta, valor, visible, onChange, onVisible,
}: {
  etiqueta: string
  valor: string
  visible: boolean
  onChange: (valor: string) => void
  onVisible: () => void
}) {
  return (
    <label>
      {etiqueta}
      <span className="clave-linea">
        <input type={visible ? 'text' : 'password'} value={valor} autoComplete="off" onChange={(event) => onChange(event.target.value)} />
        <button type="button" className="ghost" onClick={onVisible}>{visible ? 'Ocultar' : 'Ver'}</button>
      </span>
    </label>
  )
}

export function CambioClaveScreen() {
  const [politica, setPolitica] = useState<Politica | null>(null)
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [verActual, setVerActual] = useState(false)
  const [verNueva, setVerNueva] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const pendientes = faltantes(nueva, politica)

  useEffect(() => {
    api<Politica>('/api/auth/politica-clave')
      .then(setPolitica)
      .catch((reason: Error) => setError(reason.message))
  }, [])

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setAviso(null)
    if (pendientes.length > 0) {
      setError(pendientes.join(' '))
      return
    }
    try {
      await api<void>('/api/auth/cambiar-clave', {
        method: 'POST',
        body: JSON.stringify({ actual, nueva }),
      })
      setActual('')
      setNueva('')
      setAviso('Clave actualizada')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'No se pudo cambiar la clave')
    }
  }

  return (
    <section className="catalog">
      {aviso && <p className="ok">{aviso}</p>}
      {error && <p className="down">{error}</p>}
      <form onSubmit={guardar}>
        <h2>Cambio de clave</h2>
        <CampoClave etiqueta="Clave actual" valor={actual} visible={verActual} onChange={setActual} onVisible={() => setVerActual((valor) => !valor)} />
        <CampoClave etiqueta="Clave nueva" valor={nueva} visible={verNueva} onChange={setNueva} onVisible={() => setVerNueva((valor) => !valor)} />
        {nueva.length > 0 && pendientes.length > 0 && (
          <ul className="clave-requisitos">
            {pendientes.map((item) => <li key={item}>{item}</li>)}
          </ul>
        )}
        <button type="submit" disabled={pendientes.length > 0 || actual.length === 0}>Guardar</button>
      </form>
    </section>
  )
}
