import { useState } from 'react'
import { api } from './api'
import {
  dibujarPublicacion,
  type ColumnaImagen,
  type FilaImagen,
  type Formato,
} from './publicacionImagen'

type Goleador = { alias: string; nombre: string; equipo: string; goles: number }

type Resultado = {
  canchaNombre: string | null
  local: string
  visita: string
  golesLocal: number | null
  golesVisita: number | null
}

type FilaTabla = {
  posicion: number
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

type Grupo = { id: string | null; nombre: string; equipos: FilaTabla[] }

type Horario = {
  canchaNombre: string | null
  hora: string | null
  textoLocal: string
  textoVisita: string
}

const TITULOS = {
  GOLEADORES: 'GOLEADORES',
  RESULTADOS: 'RESULTADOS',
  POSICIONES: 'TABLA DE POSICIONES',
  HORARIOS: 'HORARIOS',
} as const

const COLUMNAS_TABLA = [
  { codigo: 'POSICION', titulo: 'POS', flex: 1, numero: true, destacada: false },
  { codigo: 'EQUIPO', titulo: 'EQUIPO', flex: 2.6, numero: false, destacada: false },
  { codigo: 'PJ', titulo: 'PJ', flex: 0.7, numero: false, destacada: false },
  { codigo: 'G', titulo: 'G', flex: 0.7, numero: false, destacada: false },
  { codigo: 'E', titulo: 'E', flex: 0.7, numero: false, destacada: false },
  { codigo: 'P', titulo: 'P', flex: 0.7, numero: false, destacada: false },
  { codigo: 'PTS', titulo: 'PTS', flex: 0.8, numero: false, destacada: true },
  { codigo: 'GF', titulo: 'GF', flex: 0.7, numero: false, destacada: false },
  { codigo: 'GC', titulo: 'GC', flex: 0.7, numero: false, destacada: false },
  { codigo: 'DIF', titulo: 'DIF', flex: 0.7, numero: false, destacada: true },
]

function valorFila(fila: FilaTabla, codigo: string) {
  if (codigo === 'POSICION') return String(fila.posicion)
  if (codigo === 'EQUIPO') return fila.equipo
  if (codigo === 'PJ') return String(fila.pj)
  if (codigo === 'G') return String(fila.ganados)
  if (codigo === 'E') return String(fila.empatados)
  if (codigo === 'P') return String(fila.perdidos)
  if (codigo === 'PTS') return String(fila.puntos)
  if (codigo === 'GF') return String(fila.golesFavor)
  if (codigo === 'GC') return String(fila.golesContra)
  return String(fila.diferencia)
}

function horaVisible(hora: string | null) {
  if (!hora) return ''
  return hora.slice(0, 5).replace(/^0(\d:)/, '$1')
}

function guardar(canvas: HTMLCanvasElement, nombre: string) {
  return new Promise<void>((resolve) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        resolve()
        return
      }
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = nombre
      enlace.click()
      URL.revokeObjectURL(url)
      resolve()
    }, 'image/png')
  })
}

export function BotonDescargarImagen({
  leagueId,
  tipo,
  fecha,
  deshabilitado,
  goleadores = [],
  resultados = [],
  grupos = [],
  horarios = [],
  etiqueta = 'Descargar imagen',
}: {
  leagueId: string
  tipo: keyof typeof TITULOS
  fecha: string | null
  etiqueta?: string
  deshabilitado?: boolean
  goleadores?: Goleador[]
  resultados?: Resultado[]
  grupos?: Grupo[]
  horarios?: Horario[]
}) {
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function descargar() {
    if (!leagueId) return
    setOcupado(true)
    setError(null)
    try {
      const formato = await api<Formato>(`/api/formatos-publicacion?leagueId=${leagueId}`)
      const piezas = armar(formato, tipo, goleadores, resultados, grupos, horarios)
      if (piezas.length === 0) {
        setError('No hay datos para la imagen')
        return
      }
      for (const pieza of piezas) {
        const canvas = document.createElement('canvas')
        const titulo = formato.tablas.find((tabla) => tabla.tipo === tipo)?.titulo || TITULOS[tipo]
        await dibujarPublicacion(canvas, formato, tipo, titulo, pieza.fecha ?? fecha, pieza.columnas, pieza.filas)
        const sufijo = pieza.nombre ? `-${pieza.nombre}` : ''
        const dia = fecha ?? new Date().toISOString().slice(0, 10)
        await guardar(canvas, `${tipo.toLowerCase()}-${dia}${sufijo}.png`)
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo descargar la imagen')
    } finally {
      setOcupado(false)
    }
  }

  if (!leagueId) return null

  return (
    <span className="descarga-imagen">
      <button type="button" className="con-icono" disabled={deshabilitado || ocupado} onClick={descargar}>
        <IconoImagen />
        {ocupado ? 'Generando…' : etiqueta}
      </button>
      {error && <span className="down">{error}</span>}
    </span>
  )
}

function armar(
  formato: Formato,
  tipo: keyof typeof TITULOS,
  goleadores: Goleador[],
  resultados: Resultado[],
  grupos: Grupo[],
  horarios: Horario[],
) {
  if (tipo === 'GOLEADORES') {
    return [{
      nombre: '',
      fecha: null as string | null,
      columnas: columnasGoleadores(),
      filas: filasGoleadores(goleadores),
    }]
  }
  if (tipo === 'RESULTADOS') {
    return [{
      nombre: '',
      fecha: null,
      columnas: columnasResultados(),
      filas: resultados.map((partido) => ({
        valores: [
          partido.local,
          partido.golesLocal == null ? '' : String(partido.golesLocal),
          partido.visita,
          partido.golesVisita == null ? '' : String(partido.golesVisita),
        ],
      })),
    }]
  }
  if (tipo === 'HORARIOS') {
    return [{
      nombre: '',
      fecha: null,
      columnas: columnasHorarios(),
      filas: filasHorarios(horarios),
    }]
  }
  const marcadas = new Set((formato.tablas.find((tabla) => tabla.tipo === 'POSICIONES')?.columnasTabla ?? '').split(','))
  const activas = COLUMNAS_TABLA.filter((columna) => marcadas.has(columna.codigo))
  const columnas: ColumnaImagen[] = activas.map((columna) => ({
    titulo: columna.titulo,
    destacada: columna.destacada,
    numero: columna.numero,
    flex: columna.flex,
  }))
  const lista = grupos.length > 0 ? grupos : [{ id: null, nombre: '', equipos: [] }]
  return lista.map((grupo) => ({
    nombre: grupos.length > 1 ? grupo.nombre.toLowerCase().replace(/\s+/g, '-') : '',
    fecha: null,
    columnas,
    filas: grupo.equipos.map((fila) => ({
      valores: activas.map((columna) => valorFila(fila, columna.codigo)),
    })),
  }))
}

export function filasGoleadores(goleadores: Goleador[]): FilaImagen[] {
  let fin = Math.min(6, goleadores.length)
  if (goleadores.length > 6) {
    const corte = goleadores[5].goles
    while (fin < goleadores.length && goleadores[fin].goles === corte) fin += 1
  }
  return goleadores.slice(0, fin).map((goleador, indice) => ({
    valores: [String(indice + 1), goleador.alias.trim() || goleador.nombre, String(goleador.goles), goleador.equipo],
  }))
}

function columnasGoleadores(): ColumnaImagen[] {
  return [
    { titulo: 'POS', destacada: false, numero: true, flex: 1 },
    { titulo: 'JUGADOR', destacada: false, numero: false, flex: 2.4 },
    { titulo: 'GOLES', destacada: true, numero: false, flex: 0.9 },
    { titulo: 'EQUIPO', destacada: false, numero: false, flex: 2.6 },
  ]
}

function columnasResultados(): ColumnaImagen[] {
  return [
    { titulo: 'LOCAL', destacada: false, numero: false, flex: 2.2 },
    { titulo: 'GOLES', destacada: true, numero: false, flex: 0.8 },
    { titulo: 'VISITA', destacada: false, numero: false, flex: 2.2 },
    { titulo: 'GOLES', destacada: true, numero: false, flex: 0.8 },
  ]
}

export function IconoImagen() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor" stroke="none" />
      <path d="M21 15l-5-5L5 21" />
    </svg>
  )
}

export function IconoPdf() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
      <path d="M8 13h8M8 17h5" />
    </svg>
  )
}

function columnasHorarios(): ColumnaImagen[] {
  return [
    { titulo: 'CANCHA', destacada: false, numero: false, flex: 1.4 },
    { titulo: 'HORA', destacada: true, numero: false, flex: 0.8 },
    { titulo: 'Nº', destacada: false, numero: true, flex: 0.5 },
    { titulo: 'LOCAL', destacada: false, numero: false, flex: 2.2 },
    { titulo: 'VISITA', destacada: false, numero: false, flex: 2.2 },
  ]
}

function filasHorarios(horarios: Horario[]): FilaImagen[] {
  const ordenados = [...horarios].sort((a, b) => {
    const cancha = (a.canchaNombre ?? '').localeCompare(b.canchaNombre ?? '', 'es')
    if (cancha !== 0) return cancha
    return (a.hora ?? '').localeCompare(b.hora ?? '')
  })
  let numero = 0
  let cancha = ''
  return ordenados.map((partido) => {
    const nombre = partido.canchaNombre ?? ''
    numero = nombre === cancha ? numero + 1 : 1
    cancha = nombre
    return { valores: [nombre, horaVisible(partido.hora), String(numero), partido.textoLocal, partido.textoVisita] }
  })
}
