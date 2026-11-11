import { useEffect, useRef, useState } from 'react'

export function aDia(iso: string | null | undefined) {
  if (!iso || !/^\d{4}-\d{2}-\d{2}/.test(iso)) {
    return ''
  }
  const [anio, mes, dia] = iso.slice(0, 10).split('-')
  return `${dia}/${mes}/${anio}`
}

export function aIso(texto: string) {
  const partes = texto.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!partes) {
    return null
  }
  const dia = Number(partes[1])
  const mes = Number(partes[2])
  const anio = Number(partes[3])
  const fecha = new Date(Date.UTC(anio, mes - 1, dia))
  if (fecha.getUTCFullYear() !== anio || fecha.getUTCMonth() !== mes - 1 || fecha.getUTCDate() !== dia) {
    return null
  }
  return `${partes[3]}-${partes[2]}-${partes[1]}`
}

function enmascarar(entrada: string) {
  const digitos = entrada.replace(/\D/g, '').slice(0, 8)
  if (digitos.length <= 2) {
    return digitos
  }
  if (digitos.length <= 4) {
    return `${digitos.slice(0, 2)}/${digitos.slice(2)}`
  }
  return `${digitos.slice(0, 2)}/${digitos.slice(2, 4)}/${digitos.slice(4)}`
}

export function FechaCampo({
  value,
  onChange,
  disabled,
  required,
  ariaLabel,
}: {
  value: string | null
  onChange: (iso: string | null) => void
  disabled?: boolean
  required?: boolean
  ariaLabel?: string
}) {
  const [texto, setTexto] = useState(aDia(value))
  const calendario = useRef<HTMLInputElement>(null)

  useEffect(() => {
    setTexto(aDia(value))
  }, [value])

  function aplicar(iso: string | null) {
    setTexto(aDia(iso))
    onChange(iso)
  }

  return (
    <span className="fecha-campo">
      <input
        className="fecha"
        type="text"
        inputMode="numeric"
        placeholder="dd/mm/aaaa"
        autoComplete="off"
        maxLength={10}
        aria-label={ariaLabel}
        disabled={disabled}
        required={required}
        value={texto}
        onChange={(event) => {
          const siguiente = enmascarar(event.target.value)
          setTexto(siguiente)
          if (siguiente === '') {
            event.target.setCustomValidity('')
            onChange(null)
            return
          }
          const iso = aIso(siguiente)
          if (iso) {
            event.target.setCustomValidity('')
            onChange(iso)
            return
          }
          event.target.setCustomValidity('Usa día/mes/año')
        }}
        onBlur={(event) => {
          if (texto !== '' && !aIso(texto)) {
            setTexto(aDia(value))
            event.target.setCustomValidity('')
          }
        }}
      />
      <input
        ref={calendario}
        className="fecha-nativa"
        type="date"
        tabIndex={-1}
        aria-hidden="true"
        disabled={disabled}
        value={value && aIso(aDia(value)) ? value.slice(0, 10) : ''}
        onChange={(event) => aplicar(event.target.value || null)}
      />
      <button
        type="button"
        className="fecha-abrir"
        aria-label={ariaLabel ? `Elegir ${ariaLabel}` : 'Elegir fecha'}
        disabled={disabled}
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => calendario.current?.showPicker()}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M3 10h18M8 3v4M16 3v4" />
        </svg>
      </button>
    </span>
  )
}
