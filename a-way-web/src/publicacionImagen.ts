export const MARCO_ANCHO = 1024
export const MARCO_ALTO = 818

const MESES = [
  'ENERO', 'FEBRERO', 'MARZO', 'ABRIL', 'MAYO', 'JUNIO',
  'JULIO', 'AGOSTO', 'SEPTIEMBRE', 'OCTUBRE', 'NOVIEMBRE', 'DICIEMBRE',
]

export type TablaEstilo = {
  tipo: string
  tablaX: number
  tablaY: number
  tablaAncho: number
  bordeGrosor: number
  bordeColor: string
  cabeceraAlto: number
  cabeceraFondo: string
  cabeceraColor: string
  cabeceraTamano: number
  filaAlto: number
  filaFondo: string
  filaColor: string
  filaTamano: number
  numeroColor: string
  numeroTamano: number
  destacadaFondo: string
  columnasTabla: string
  titulo: string
  tituloX: number
  tituloY: number
  tituloTamano: number
  subtituloX: number
  subtituloY: number
  subtituloTamano: number
  fechaX: number
  fechaY: number
  fechaTamano: number
  lugarY: number
  lugarTamano: number
  agruparPrimeraColumna: boolean
  ajustarAnchoTexto: boolean
  fondoPublicacion: string
}

export type Formato = {
  id: string
  leagueId: string
  leagueName: string
  imagenPlantilla: string
  hasLogo: boolean
  nombreLugar: string
  descripcion: string
  logoX: number
  logoY: number
  logoAncho: number
  logoAlto: number
  logoHabilitado: boolean
  tituloHabilitado: boolean
  subtituloHabilitado: boolean
  lugarHabilitado: boolean
  fechaHabilitado: boolean
  tituloNegrita: boolean
  tituloCursiva: boolean
  tituloFamilia: string
  tituloColorInicio: string
  tituloColorFin: string
  tituloContornoColor: string
  tituloContornoGrosor: number
  tituloSombra: boolean
  subtituloNegrita: boolean
  subtituloCursiva: boolean
  subtituloFamilia: string
  subtituloColor: string
  lugarCursiva: boolean
  lugarFamilia: string
  lugarColor: string
  lugarRayaAncho: number
  lugarRayaGrosor: number
  fechaAncho: number
  fechaAlto: number
  fechaRadio: number
  fechaBordeGrosor: number
  fechaBordeColor: string
  fechaNegrita: boolean
  fechaFamilia: string
  fechaColor: string
  fechaIconoTamano: number
  tablas: TablaEstilo[]
}

export type ColumnaImagen = {
  titulo: string
  destacada: boolean
  numero: boolean
  flex: number
}

export type FilaImagen = {
  valores: string[]
}

export function fechaLarga(iso: string | null) {
  if (!iso || !/^\d{4}-\d{2}-\d{2}/.test(iso)) {
    return 'FECHA:'
  }
  const [, mes, dia] = iso.slice(0, 10).split('-')
  return `FECHA: ${Number(dia)} DE ${MESES[Number(mes) - 1]}`
}

async function cargar(url: string) {
  try {
    const respuesta = await fetch(url, { credentials: 'include' })
    if (!respuesta.ok) {
      return null
    }
    return await createImageBitmap(await respuesta.blob())
  } catch {
    return null
  }
}

function fuente(tamano: number, familia: string, negrita: boolean, cursiva: boolean) {
  const peso = negrita ? 700 : 400
  const estilo = cursiva ? 'italic' : 'normal'
  return `${estilo} ${peso} ${Math.round(tamano)}px "${familia}"`
}

function dibujarCalendario(ctx: CanvasRenderingContext2D, x: number, y: number, tamano: number, color: string) {
  ctx.save()
  ctx.translate(x, y)
  ctx.scale(tamano / 24, tamano / 24)
  ctx.strokeStyle = color
  ctx.lineWidth = 2
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.beginPath()
  ctx.roundRect(3, 5, 18, 16, 2)
  ctx.moveTo(3, 10)
  ctx.lineTo(21, 10)
  ctx.moveTo(8, 3)
  ctx.lineTo(8, 7)
  ctx.moveTo(16, 3)
  ctx.lineTo(16, 7)
  ctx.stroke()
  ctx.restore()
}

function recortar(ctx: CanvasRenderingContext2D, texto: string, ancho: number) {
  if (ctx.measureText(texto).width <= ancho) {
    return texto
  }
  let corte = texto
  while (corte.length > 1 && ctx.measureText(`${corte}…`).width > ancho) {
    corte = corte.slice(0, -1)
  }
  return `${corte}…`
}

function anchosDeColumnas(
  ctx: CanvasRenderingContext2D,
  tabla: TablaEstilo,
  columnas: ColumnaImagen[],
  filas: FilaImagen[],
  anchoTabla: number,
  altoFila: number,
  px: (valor: number) => number,
) {
  if (!tabla.ajustarAnchoTexto) {
    const flex = columnas.reduce((suma, columna) => suma + columna.flex, 0) || 1
    return columnas.map((columna) => (anchoTabla * columna.flex) / flex)
  }
  const holgura = px(20)
  const anchos = columnas.map((columna, indice) => {
    ctx.font = fuente(px(tabla.cabeceraTamano), 'Arial', true, false)
    let maximo = ctx.measureText(columna.titulo).width
    const tamano = Math.min(columna.numero ? px(tabla.numeroTamano) : px(tabla.filaTamano), altoFila * 0.72)
    ctx.font = fuente(tamano, 'Arial', Boolean(columna.numero), false)
    filas.forEach((fila) => {
      maximo = Math.max(maximo, ctx.measureText(fila.valores[indice] ?? '').width)
    })
    return Math.max(px(28), maximo + holgura)
  })
  const total = anchos.reduce((suma, item) => suma + item, 0)
  if (total > anchoTabla && total > 0) {
    const factor = anchoTabla / total
    return anchos.map((item) => item * factor)
  }
  return anchos
}

export async function dibujarPublicacion(
  canvas: HTMLCanvasElement,
  formato: Formato,
  tipo: string,
  titulo: string,
  fechaIso: string | null,
  columnas: ColumnaImagen[],
  filas: FilaImagen[],
  vigente: () => boolean = () => true,
) {
  const tabla = formato.tablas.find((item) => item.tipo === tipo)
  if (!tabla) {
    throw new Error('Falta el estilo de la tabla')
  }
  const archivoFondo = (tabla.fondoPublicacion || '').trim() || formato.imagenPlantilla
  const fondo = await cargar(`/plantillas/${archivoFondo}`)
  if (!vigente()) return
  if (!fondo) {
    throw new Error('No se encontró el fondo de la publicación')
  }
  const logo = formato.hasLogo ? await cargar(`/api/leagues/${formato.leagueId}/logo`) : null
  if (!vigente()) return

  canvas.width = fondo.width
  canvas.height = fondo.height
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('No se pudo preparar la imagen')
  }
  const sx = canvas.width / MARCO_ANCHO
  const sy = canvas.height / MARCO_ALTO
  const px = (valor: number) => valor * sx
  const py = (valor: number) => valor * sy

  ctx.drawImage(fondo, 0, 0)
  if (formato.logoHabilitado !== false && logo) {
    ctx.drawImage(logo, px(formato.logoX), py(formato.logoY), px(formato.logoAncho), py(formato.logoAlto))
  }

  if (formato.tituloHabilitado !== false) {
    const tituloTamano = px(tabla.tituloTamano)
    ctx.font = fuente(tituloTamano, formato.tituloFamilia, formato.tituloNegrita, formato.tituloCursiva)
    ctx.textAlign = 'left'
    ctx.textBaseline = 'alphabetic'
    const tituloX = px(tabla.tituloX)
    const tituloBase = py(tabla.tituloY) + tituloTamano * 0.85
    ctx.lineJoin = 'round'
    ctx.miterLimit = 2
    ctx.lineWidth = px(formato.tituloContornoGrosor)
    ctx.strokeStyle = formato.tituloContornoColor
    if (formato.tituloSombra) {
      ctx.shadowColor = 'rgba(0, 0, 0, 0.45)'
      ctx.shadowBlur = px(4)
      ctx.shadowOffsetX = px(2)
      ctx.shadowOffsetY = px(2)
    }
    if (formato.tituloContornoGrosor > 0) {
      ctx.strokeText(titulo, tituloX, tituloBase)
    }
    ctx.shadowColor = 'transparent'
    const degradado = ctx.createLinearGradient(tituloX, tituloBase - tituloTamano, tituloX, tituloBase)
    degradado.addColorStop(0, formato.tituloColorInicio)
    degradado.addColorStop(1, formato.tituloColorFin)
    ctx.fillStyle = degradado
    ctx.fillText(titulo, tituloX, tituloBase)
  }

  const descripcion = formato.descripcion.trim()
  if (formato.subtituloHabilitado !== false && descripcion) {
    const subtituloTamano = px(tabla.subtituloTamano)
    ctx.font = fuente(subtituloTamano, formato.subtituloFamilia, formato.subtituloNegrita, formato.subtituloCursiva)
    ctx.fillStyle = formato.subtituloColor
    ctx.fillText(descripcion, px(tabla.subtituloX), py(tabla.subtituloY) + subtituloTamano * 0.85)
  }

  const lugar = formato.nombreLugar.trim()
  if (formato.lugarHabilitado !== false && lugar) {
    const lugarTamano = px(tabla.lugarTamano)
    ctx.font = fuente(lugarTamano, formato.lugarFamilia, false, formato.lugarCursiva)
    ctx.fillStyle = formato.lugarColor
    ctx.textAlign = 'center'
    const centro = canvas.width / 2
    const lugarBase = py(tabla.lugarY) + lugarTamano * 0.85
    ctx.fillText(lugar, centro, lugarBase)
    const mitad = ctx.measureText(lugar).width / 2
    const separacion = px(16)
    const raya = px(formato.lugarRayaAncho)
    ctx.strokeStyle = formato.lugarColor
    ctx.lineWidth = Math.max(1, px(formato.lugarRayaGrosor))
    ctx.beginPath()
    ctx.moveTo(centro - mitad - separacion - raya, lugarBase - lugarTamano * 0.3)
    ctx.lineTo(centro - mitad - separacion, lugarBase - lugarTamano * 0.3)
    ctx.moveTo(centro + mitad + separacion, lugarBase - lugarTamano * 0.3)
    ctx.lineTo(centro + mitad + separacion + raya, lugarBase - lugarTamano * 0.3)
    ctx.stroke()
    ctx.textAlign = 'left'
  }

  if (formato.fechaHabilitado !== false) {
    const fechaX = px(tabla.fechaX)
    const fechaY = py(tabla.fechaY)
    const fechaAncho = px(formato.fechaAncho)
    const fechaAlto = py(formato.fechaAlto)
    ctx.beginPath()
    ctx.roundRect(fechaX, fechaY, fechaAncho, fechaAlto, px(formato.fechaRadio))
    ctx.lineWidth = Math.max(1, px(formato.fechaBordeGrosor))
    ctx.strokeStyle = formato.fechaBordeColor
    ctx.stroke()
    const iconoTamano = px(formato.fechaIconoTamano)
    dibujarCalendario(
      ctx,
      fechaX + px(16),
      fechaY + (fechaAlto - iconoTamano) / 2,
      iconoTamano,
      formato.fechaColor,
    )
    const fechaTamano = px(tabla.fechaTamano)
    ctx.font = fuente(fechaTamano, formato.fechaFamilia, formato.fechaNegrita, false)
    ctx.fillStyle = formato.fechaColor
    ctx.textAlign = 'center'
    ctx.fillText(fechaLarga(fechaIso), fechaX + fechaAncho / 2 + iconoTamano / 4, fechaY + fechaAlto / 2 + fechaTamano * 0.35)
    ctx.textAlign = 'left'
  }

  if (columnas.length === 0) {
    return
  }
  const izquierda = px(tabla.tablaX)
  const arriba = py(tabla.tablaY)
  const ancho = px(tabla.tablaAncho)
  const disponible = canvas.height - arriba - py(16)
  const altoCabecera = py(tabla.cabeceraAlto)
  let altoFila = py(tabla.filaAlto)
  const minimo = py(22)
  if (filas.length > 0 && altoCabecera + filas.length * altoFila > disponible) {
    altoFila = Math.max(minimo, (disponible - altoCabecera) / filas.length)
  }
  const cupo = Math.max(0, Math.floor((disponible - altoCabecera) / Math.max(altoFila, 1)))
  const visibles = filas.slice(0, cupo)
  const alto = altoCabecera + visibles.length * altoFila
  const anchos = anchosDeColumnas(ctx, tabla, columnas, visibles, ancho, altoFila, px)
  const anchoTabla = anchos.reduce((suma, item) => suma + item, 0)

  ctx.textBaseline = 'middle'
  ctx.lineWidth = Math.max(1, px(tabla.bordeGrosor))
  ctx.strokeStyle = tabla.bordeColor
  ctx.font = fuente(px(tabla.cabeceraTamano), 'Arial', true, false)
  let cursor = izquierda
  columnas.forEach((columna, indice) => {
    ctx.fillStyle = tabla.cabeceraFondo
    ctx.fillRect(cursor, arriba, anchos[indice], altoCabecera)
    ctx.fillStyle = tabla.cabeceraColor
    ctx.textAlign = 'center'
    ctx.fillText(recortar(ctx, columna.titulo, anchos[indice] - px(8)), cursor + anchos[indice] / 2, arriba + altoCabecera / 2)
    cursor += anchos[indice]
  })

  const agrupar = tabla.agruparPrimeraColumna && columnas.length > 0
  visibles.forEach((fila, indiceFila) => {
    const filaY = arriba + altoCabecera + indiceFila * altoFila
    let x = izquierda
    fila.valores.forEach((valor, indice) => {
      const columna = columnas[indice]
      const inicioGrupo = indice === 0 && agrupar && (indiceFila === 0 || visibles[indiceFila - 1].valores[0] !== valor)
      const sigueGrupo = indice === 0 && agrupar && indiceFila > 0 && visibles[indiceFila - 1].valores[0] === valor
      if (sigueGrupo) {
        x += anchos[indice]
        return
      }
      let altoCelda = altoFila
      if (inicioGrupo) {
        let span = 1
        while (indiceFila + span < visibles.length && visibles[indiceFila + span].valores[0] === valor) span += 1
        altoCelda = altoFila * span
      }
      ctx.fillStyle = columna.destacada ? tabla.destacadaFondo : tabla.filaFondo
      ctx.fillRect(x, filaY, anchos[indice], altoCelda)
      const tamano = columna.numero ? px(tabla.numeroTamano) : px(tabla.filaTamano)
      ctx.font = fuente(Math.min(tamano, altoFila * 0.72), 'Arial', columna.numero, false)
      ctx.fillStyle = columna.numero ? tabla.numeroColor : tabla.filaColor
      const centrada = columna.numero || columna.destacada || (tipo === 'POSICIONES' && columna.titulo !== 'EQUIPO')
      ctx.textAlign = centrada ? 'center' : 'left'
      const texto = recortar(ctx, valor, anchos[indice] - px(16))
      const textoX = centrada ? x + anchos[indice] / 2 : x + px(10)
      ctx.fillText(texto, textoX, filaY + altoCelda / 2)
      x += anchos[indice]
    })
  })

  ctx.beginPath()
  ctx.rect(izquierda, arriba, anchoTabla, alto)
  for (let indice = 1; indice < columnas.length; indice += 1) {
    const x = izquierda + anchos.slice(0, indice).reduce((suma, item) => suma + item, 0)
    ctx.moveTo(x, arriba)
    ctx.lineTo(x, arriba + alto)
  }
  ctx.moveTo(izquierda, arriba + altoCabecera)
  ctx.lineTo(izquierda + anchoTabla, arriba + altoCabecera)
  for (let indice = 1; indice <= visibles.length; indice += 1) {
    const y = arriba + altoCabecera + indice * altoFila
    const cortaPrimera = agrupar && indice < visibles.length && visibles[indice].valores[0] === visibles[indice - 1].valores[0]
    ctx.moveTo(cortaPrimera ? izquierda + anchos[0] : izquierda, y)
    ctx.lineTo(izquierda + anchoTabla, y)
  }
  ctx.stroke()
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'
}
