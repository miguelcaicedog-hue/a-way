import { useEffect, useRef } from 'react'

const PRIMER_CAMPO = [
  'input:not([type="hidden"]):not([type="file"]):not([type="checkbox"]):not(:disabled)',
  'select:not(:disabled)',
  'textarea:not(:disabled)',
].join(', ')

export function useFocoEdicion(marca: string | null) {
  const form = useRef<HTMLFormElement>(null)
  useEffect(() => {
    if (!marca) {
      return
    }
    form.current?.querySelector<HTMLElement>(PRIMER_CAMPO)?.focus()
  }, [marca])
  return form
}
