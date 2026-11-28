import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { api } from './api'
import { FechaCampo } from './Fecha'
import {
  dibujarPublicacion,
  type ColumnaImagen,
  type FilaImagen,
  type Formato,
  type TablaEstilo,
} from './publicacionImagen'

type Profile = {
  master: boolean
  memberships: { role: string }[]
}

type Categoria = { id: string; name: string }

type Goleador = { alias: string; nombre: string; equipo: string; goles: number }

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

type Partido = {
  canchaNombre: string | null
  local: string
  visita: string
  golesLocal: number | null
  golesVisita: number | null
}

type Horario = {
  canchaNombre: string | null
  hora: string | null
  textoLocal: string
  textoVisita: string
}

const TIPOS = [
  { id: 'GOLEADORES', titulo: 'GOLEADORES', etiqueta: 'Goleadores' },
  { id: 'RESULTADOS', titulo: 'RESULTADOS', etiqueta: 'Resultados' },
  { id: 'POSICIONES', titulo: 'TABLA DE POSICIONES', etiqueta: 'Tabla de posiciones' },
  { id: 'HORARIOS', titulo: 'HORARIOS', etiqueta: 'Horarios de partidos' },
] as const

const LETRAS = [
  'Arial',
  'Arial Black',
  'Calibri',
  'Cambria',
  'Candara',
  'Comic Sans MS',
  'Consolas',
  'Constantia',
  'Corbel',
  'Courier New',
  'Georgia',
  'Impact',
  'Palatino Linotype',
  'Segoe UI',
  'Tahoma',
  'Times New Roman',
  'Trebuchet MS',
  'Verdana',
]

const COLUMNAS_TABLA = [
  { codigo: 'POSICION', etiqueta: 'Posición', titulo: '#', flex: 0.7, numero: true, destacada: false },
  { codigo: 'EQUIPO', etiqueta: 'Equipo', titulo: 'EQUIPO', flex: 2.6, numero: false, destacada: false },
  { codigo: 'PJ', etiqueta: 'PJ', titulo: 'PJ', flex: 0.7, numero: false, destacada: false },
  { codigo: 'G', etiqueta: 'G', titulo: 'G', flex: 0.7, numero: false, destacada: false },
  { codigo: 'E', etiqueta: 'E', titulo: 'E', flex: 0.7, numero: false, destacada: false },
  { codigo: 'P', etiqueta: 'P', titulo: 'P', flex: 0.7, numero: false, destacada: false },
  { codigo: 'PTS', etiqueta: 'Pts', titulo: 'PTS', flex: 0.8, numero: false, destacada: true },
  { codigo: 'GF', etiqueta: 'GF', titulo: 'GF', flex: 0.7, numero: false, destacada: false },
  { codigo: 'GC', etiqueta: 'GC', titulo: 'GC', flex: 0.7, numero: false, destacada: false },
  { codigo: 'DIF', etiqueta: 'Dif', titulo: 'DIF', flex: 0.7, numero: false, destacada: false },
]

function horaVisible(hora: string | null) {
  if (!hora) return ''
  return hora.slice(0, 5).replace(/^0(\d:)/, '$1')
}

function canWrite(profile: Profile) {
  return profile.master || profile.memberships.some((item) => item.role === 'ADMINISTRATIVO')
}

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

function Numero({
  etiqueta, valor, disabled, onChange,
}: { etiqueta: string; valor: number; disabled: boolean; onChange: (valor: number) => void }) {
  return (
    <label>
      {etiqueta}
      <input type="number" value={valor} disabled={disabled} onChange={(event) => onChange(Number(event.target.value))} />
    </label>
  )
}

function ColorCampo({
  etiqueta, valor, disabled, onChange,
}: { etiqueta: string; valor: string; disabled: boolean; onChange: (valor: string) => void }) {
  return (
    <label>
      {etiqueta}
      <span className="color-campo">
        <input type="color" value={valor.toLowerCase()} disabled={disabled} onChange={(event) => onChange(event.target.value.toUpperCase())} />
        <span>{valor.toUpperCase()}</span>
      </span>
    </label>
  )
}

function Grupo({
  titulo, nota, children, plano = false, abierto = false,
}: { titulo: string; nota?: string; children: ReactNode; plano?: boolean; abierto?: boolean }) {
  const nodo = useRef<HTMLDetailsElement>(null)
  useEffect(() => {
    if (abierto && nodo.current) nodo.current.open = true
  }, [abierto])
  return (
    <details ref={nodo} className="formato-grupo">
      <summary>{titulo}</summary>
      {nota && <p className="formato-nota">{nota}</p>}
      {plano ? children : <div className="formato-campos">{children}</div>}
    </details>
  )
}

function Letra({
  etiqueta, valor, disabled, onChange,
}: { etiqueta: string; valor: string; disabled: boolean; onChange: (valor: string) => void }) {
  const opciones = LETRAS.includes(valor) ? LETRAS : [valor, ...LETRAS]
  return (
    <label>
      {etiqueta}
      <select value={valor} disabled={disabled} style={{ fontFamily: valor }} onChange={(event) => onChange(event.target.value)}>
        {opciones.map((fuente) => (
          <option key={fuente} value={fuente} style={{ fontFamily: fuente }}>{fuente}</option>
        ))}
      </select>
    </label>
  )
}

function Marca({
  etiqueta, valor, disabled, onChange,
}: { etiqueta: string; valor: boolean; disabled: boolean; onChange: (valor: boolean) => void }) {
  return (
    <label className="check">
      <input type="checkbox" checked={valor} disabled={disabled} onChange={(event) => onChange(event.target.checked)} />
      {etiqueta}
    </label>
  )
}

export function PublicacionesScreen({
  profile, leagueId, editionId,
}: { profile: Profile; leagueId: string; editionId: string }) {
  const [formato, setFormato] = useState<Formato | null>(null)
  const [tipo, setTipo] = useState<(typeof TIPOS)[number]['id']>('GOLEADORES')
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [fecha, setFecha] = useState<string | null>(null)
  const [goleadores, setGoleadores] = useState<Goleador[]>([])
  const [grupos, setGrupos] = useState<Grupo[]>([])
  const [grupoId, setGrupoId] = useState('')
  const [partidos, setPartidos] = useState<Partido[]>([])
  const [horarios, setHorarios] = useState<Horario[]>([])
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const lienzo = useRef<HTMLCanvasElement>(null)
  const serie = useRef(0)
  const writable = canWrite(profile)

  useEffect(() => {
    setFormato(null)
    setError(null)
    if (!leagueId) return
    let activo = true
    api<Formato>(`/api/formatos-publicacion?leagueId=${leagueId}`)
      .then((respuesta) => {
        if (activo) setFormato(respuesta)
      })
      .catch((caught: Error) => {
        if (activo) setError(caught.message)
      })
    return () => {
      activo = false
    }
  }, [leagueId])

  useEffect(() => {
    setCategorias([])
    setCategoryId('')
    setFecha(null)
    if (!editionId) return
    let activo = true
    api<Categoria[]>(`/api/categories?editionId=${editionId}`)
      .then((items) => {
        if (!activo) return
        setCategorias(items)
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
  }, [editionId, categoryId])

  useEffect(() => {
    if (!editionId || !categoryId || !fecha) {
      setGoleadores([])
      setGrupos([])
      setPartidos([])
      setHorarios([])
      return
    }
    let activo = true
    const params = new URLSearchParams({ editionId, categoryId, fecha })
    const consulta = tipo === 'GOLEADORES'
      ? api<{ goleadores: Goleador[] }>(`/api/goleadores?${params.toString()}`).then((panel) => {
          if (activo) setGoleadores(panel.goleadores)
        })
      : tipo === 'RESULTADOS'
        ? api<{ partidos: Partido[] }>(`/api/resultados?${params.toString()}`).then((panel) => {
            if (activo) setPartidos(panel.partidos)
          })
        : tipo === 'HORARIOS'
          ? api<{ partidos: Horario[] }>(`/api/horarios?${params.toString()}`).then((panel) => {
              if (activo) setHorarios(panel.partidos)
            })
          : api<{ grupos: Grupo[] }>(`/api/tabla?${params.toString()}`).then((panel) => {
            if (!activo) return
            setGrupos(panel.grupos)
            setGrupoId((actual) => (
              panel.grupos.some((grupo) => (grupo.id ?? grupo.nombre) === actual)
                ? actual
                : (panel.grupos[0]?.id ?? panel.grupos[0]?.nombre ?? '')
            ))
          })
    consulta.catch((caught: Error) => {
      if (activo) setError(caught.message)
    })
    return () => {
      activo = false
    }
  }, [editionId, categoryId, fecha, tipo])

  const columnas = useMemo<ColumnaImagen[]>(() => {
    if (tipo === 'GOLEADORES') {
      return [
        { titulo: '#', destacada: false, numero: true, flex: 0.7 },
        { titulo: 'JUGADOR', destacada: false, numero: false, flex: 2.4 },
        { titulo: 'GOLES', destacada: true, numero: false, flex: 0.9 },
        { titulo: 'EQUIPO', destacada: false, numero: false, flex: 2.6 },
      ]
    }
    if (tipo === 'RESULTADOS') {
      return [
        { titulo: 'CANCHA', destacada: false, numero: false, flex: 1.4 },
        { titulo: 'LOCAL', destacada: false, numero: false, flex: 2.2 },
        { titulo: 'GOLES', destacada: true, numero: false, flex: 0.8 },
        { titulo: 'VISITA', destacada: false, numero: false, flex: 2.2 },
        { titulo: 'GOLES', destacada: true, numero: false, flex: 0.8 },
      ]
    }
    if (tipo === 'HORARIOS') {
      return [
        { titulo: 'CANCHA', destacada: false, numero: false, flex: 1.4 },
        { titulo: 'HORA', destacada: true, numero: false, flex: 0.8 },
        { titulo: 'Nº', destacada: false, numero: true, flex: 0.5 },
        { titulo: 'LOCAL', destacada: false, numero: false, flex: 2.2 },
        { titulo: 'VISITA', destacada: false, numero: false, flex: 2.2 },
      ]
    }
    const marcadas = new Set((formato?.tablas.find((tabla) => tabla.tipo === 'POSICIONES')?.columnasTabla ?? '').split(','))
    return COLUMNAS_TABLA.filter((columna) => marcadas.has(columna.codigo)).map((columna) => ({
      titulo: columna.titulo,
      destacada: columna.destacada,
      numero: columna.numero,
      flex: columna.flex,
    }))
  }, [tipo, formato])

  const filas = useMemo<FilaImagen[]>(() => {
    if (tipo === 'GOLEADORES') {
      return goleadores.map((goleador, indice) => ({
        valores: [String(indice + 1), goleador.alias.trim() || goleador.nombre, String(goleador.goles), goleador.equipo],
      }))
    }
    if (tipo === 'RESULTADOS') {
      return partidos.map((partido) => ({
        valores: [
          partido.canchaNombre ?? '',
          partido.local,
          partido.golesLocal == null ? '' : String(partido.golesLocal),
          partido.visita,
          partido.golesVisita == null ? '' : String(partido.golesVisita),
        ],
      }))
    }
    if (tipo === 'HORARIOS') {
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
        return {
          valores: [nombre, horaVisible(partido.hora), String(numero), partido.textoLocal, partido.textoVisita],
        }
      })
    }
    const grupo = grupos.find((item) => (item.id ?? item.nombre) === grupoId) ?? grupos[0]
    const marcadas = new Set((formato?.tablas.find((tabla) => tabla.tipo === 'POSICIONES')?.columnasTabla ?? '').split(','))
    const activas = COLUMNAS_TABLA.filter((columna) => marcadas.has(columna.codigo))
    return (grupo?.equipos ?? []).map((fila) => ({
      valores: activas.map((columna) => valorFila(fila, columna.codigo)),
    }))
  }, [tipo, goleadores, partidos, horarios, grupos, grupoId, formato])

  useEffect(() => {
    const nodo = lienzo.current
    if (!nodo || !formato) return
    const token = serie.current + 1
    serie.current = token
    const titulo = formato.tablas.find((tabla) => tabla.tipo === tipo)?.titulo ?? ''
    dibujarPublicacion(nodo, formato, tipo, titulo, fecha, columnas, filas, () => serie.current === token)
      .then(() => {
        if (serie.current === token) {
          setError((actual) => (actual && /is not defined|fondo de la publicación|plantilla|estilo de la tabla|preparar la imagen/.test(actual) ? null : actual))
        }
      })
      .catch((caught: Error) => {
        if (serie.current === token) setError(caught.message)
      })
  }, [formato, tipo, fecha, columnas, filas])

  function cambiar<K extends keyof Formato>(campo: K, valor: Formato[K]) {
    setFormato((actual) => (actual ? { ...actual, [campo]: valor } : actual))
  }

  function cambiarTabla<K extends keyof TablaEstilo>(tipoTabla: string, campo: K, valor: TablaEstilo[K]) {
    setFormato((actual) => {
      if (!actual) return actual
      return {
        ...actual,
        tablas: actual.tablas.map((tabla) => (tabla.tipo === tipoTabla ? { ...tabla, [campo]: valor } : tabla)),
      }
    })
  }

  function marcarColumna(codigo: string, activo: boolean) {
    setFormato((actual) => {
      if (!actual) return actual
      return {
        ...actual,
        tablas: actual.tablas.map((tabla) => {
          if (tabla.tipo !== 'POSICIONES') return tabla
          const actuales = new Set(tabla.columnasTabla.split(',').filter(Boolean))
          if (activo) actuales.add(codigo)
          else actuales.delete(codigo)
          const columnasTabla = COLUMNAS_TABLA.filter((columna) => actuales.has(columna.codigo)).map((columna) => columna.codigo).join(',')
          return { ...tabla, columnasTabla }
        }),
      }
    })
  }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    if (!formato || !writable) return
    const posiciones = formato.tablas.find((tabla) => tabla.tipo === 'POSICIONES')
    if (!posiciones?.columnasTabla) {
      setError('Marca al menos una columna de la tabla de posiciones')
      return
    }
    setGuardando(true)
    setError(null)
    setAviso(null)
    try {
      setFormato(await api<Formato>(`/api/formatos-publicacion?leagueId=${leagueId}`, {
        method: 'PUT',
        body: JSON.stringify(formato),
      }))
      setAviso('Formato guardado')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No se pudo guardar el formato')
    } finally {
      setGuardando(false)
    }
  }

  function descargar() {
    const nodo = lienzo.current
    if (!nodo) return
    const titulo = TIPOS.find((item) => item.id === tipo)?.id.toLowerCase() ?? 'publicacion'
    nodo.toBlob((blob) => {
      if (!blob) return
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `${titulo}${fecha ? `-${fecha}` : ''}.png`
      enlace.click()
      URL.revokeObjectURL(url)
    }, 'image/png')
  }

  const posiciones = formato?.tablas.find((tabla) => tabla.tipo === 'POSICIONES')
  const marcadas = new Set((posiciones?.columnasTabla ?? '').split(','))

  return (
    <section className="catalog">
      {aviso && <p className="ok">{aviso}</p>}
      {!leagueId && <p>Selecciona una liga.</p>}
      {formato && (
        <form className="formato" onSubmit={guardar}>
          <h2>Estilo</h2>
          <p>Cada grupo corresponde a un bloque de la imagen. Las medidas están en píxeles del diseño de 1024 × 818.</p>
          <div className="formato-bloques">
            <Grupo titulo="Logo">
              <Marca etiqueta="Habilitar" valor={formato.logoHabilitado} disabled={!writable} onChange={(valor) => cambiar('logoHabilitado', valor)} />
              <Numero etiqueta="Posición X" valor={formato.logoX} disabled={!writable} onChange={(valor) => cambiar('logoX', valor)} />
              <Numero etiqueta="Posición Y" valor={formato.logoY} disabled={!writable} onChange={(valor) => cambiar('logoY', valor)} />
              <Numero etiqueta="Ancho" valor={formato.logoAncho} disabled={!writable} onChange={(valor) => cambiar('logoAncho', valor)} />
              <Numero etiqueta="Alto" valor={formato.logoAlto} disabled={!writable} onChange={(valor) => cambiar('logoAlto', valor)} />
            </Grupo>
            <Grupo titulo="Título" nota="Letra, colores y contorno son comunes. El texto, el tamaño y la posición están en cada tabla.">
              <Marca etiqueta="Habilitar" valor={formato.tituloHabilitado} disabled={!writable} onChange={(valor) => cambiar('tituloHabilitado', valor)} />
              <Letra etiqueta="Letra" valor={formato.tituloFamilia} disabled={!writable} onChange={(valor) => cambiar('tituloFamilia', valor)} />
              <ColorCampo etiqueta="Color inicial" valor={formato.tituloColorInicio} disabled={!writable} onChange={(valor) => cambiar('tituloColorInicio', valor)} />
              <ColorCampo etiqueta="Color final" valor={formato.tituloColorFin} disabled={!writable} onChange={(valor) => cambiar('tituloColorFin', valor)} />
              <ColorCampo etiqueta="Color del contorno" valor={formato.tituloContornoColor} disabled={!writable} onChange={(valor) => cambiar('tituloContornoColor', valor)} />
              <Numero etiqueta="Grosor del contorno" valor={formato.tituloContornoGrosor} disabled={!writable} onChange={(valor) => cambiar('tituloContornoGrosor', valor)} />
              <div className="formato-marcas">
                <Marca etiqueta="Negrita" valor={formato.tituloNegrita} disabled={!writable} onChange={(valor) => cambiar('tituloNegrita', valor)} />
                <Marca etiqueta="Cursiva" valor={formato.tituloCursiva} disabled={!writable} onChange={(valor) => cambiar('tituloCursiva', valor)} />
                <Marca etiqueta="Sombra" valor={formato.tituloSombra} disabled={!writable} onChange={(valor) => cambiar('tituloSombra', valor)} />
              </div>
            </Grupo>
            <Grupo titulo="Subtítulo" nota="La descripción, la letra y el color son comunes. El tamaño y la posición están en cada tabla. Si la descripción queda vacía o el bloque no está habilitado, la imagen no muestra esta línea.">
              <Marca etiqueta="Habilitar" valor={formato.subtituloHabilitado} disabled={!writable} onChange={(valor) => cambiar('subtituloHabilitado', valor)} />
              <label className="formato-texto">
                Descripción
                <input value={formato.descripcion} disabled={!writable} onChange={(event) => cambiar('descripcion', event.target.value)} />
              </label>
              <Letra etiqueta="Letra" valor={formato.subtituloFamilia} disabled={!writable} onChange={(valor) => cambiar('subtituloFamilia', valor)} />
              <ColorCampo etiqueta="Color" valor={formato.subtituloColor} disabled={!writable} onChange={(valor) => cambiar('subtituloColor', valor)} />
              <div className="formato-marcas">
                <Marca etiqueta="Negrita" valor={formato.subtituloNegrita} disabled={!writable} onChange={(valor) => cambiar('subtituloNegrita', valor)} />
                <Marca etiqueta="Cursiva" valor={formato.subtituloCursiva} disabled={!writable} onChange={(valor) => cambiar('subtituloCursiva', valor)} />
              </div>
            </Grupo>
            <Grupo titulo="Nombre de lugar" nota="El texto, la letra, el color y las rayas son comunes. El tamaño y la posición están en cada tabla. Si el texto queda vacío o el bloque no está habilitado, la imagen no muestra las rayas.">
              <Marca etiqueta="Habilitar" valor={formato.lugarHabilitado} disabled={!writable} onChange={(valor) => cambiar('lugarHabilitado', valor)} />
              <label>
                Texto
                <input value={formato.nombreLugar} disabled={!writable} onChange={(event) => cambiar('nombreLugar', event.target.value)} />
              </label>
              <Letra etiqueta="Letra" valor={formato.lugarFamilia} disabled={!writable} onChange={(valor) => cambiar('lugarFamilia', valor)} />
              <ColorCampo etiqueta="Color" valor={formato.lugarColor} disabled={!writable} onChange={(valor) => cambiar('lugarColor', valor)} />
              <Numero etiqueta="Ancho de cada raya" valor={formato.lugarRayaAncho} disabled={!writable} onChange={(valor) => cambiar('lugarRayaAncho', valor)} />
              <Numero etiqueta="Grosor de la raya" valor={formato.lugarRayaGrosor} disabled={!writable} onChange={(valor) => cambiar('lugarRayaGrosor', valor)} />
              <div className="formato-marcas">
                <Marca etiqueta="Cursiva" valor={formato.lugarCursiva} disabled={!writable} onChange={(valor) => cambiar('lugarCursiva', valor)} />
              </div>
            </Grupo>
            <Grupo titulo="Fecha" nota="La pastilla, la letra y el color son comunes. El tamaño del texto y la posición están en cada tabla.">
              <Marca etiqueta="Habilitar" valor={formato.fechaHabilitado} disabled={!writable} onChange={(valor) => cambiar('fechaHabilitado', valor)} />
              <Letra etiqueta="Letra" valor={formato.fechaFamilia} disabled={!writable} onChange={(valor) => cambiar('fechaFamilia', valor)} />
              <Numero etiqueta="Ancho" valor={formato.fechaAncho} disabled={!writable} onChange={(valor) => cambiar('fechaAncho', valor)} />
              <Numero etiqueta="Alto" valor={formato.fechaAlto} disabled={!writable} onChange={(valor) => cambiar('fechaAlto', valor)} />
              <Numero etiqueta="Radio de la esquina" valor={formato.fechaRadio} disabled={!writable} onChange={(valor) => cambiar('fechaRadio', valor)} />
              <Numero etiqueta="Grosor del borde" valor={formato.fechaBordeGrosor} disabled={!writable} onChange={(valor) => cambiar('fechaBordeGrosor', valor)} />
              <ColorCampo etiqueta="Color del borde" valor={formato.fechaBordeColor} disabled={!writable} onChange={(valor) => cambiar('fechaBordeColor', valor)} />
              <ColorCampo etiqueta="Color del texto" valor={formato.fechaColor} disabled={!writable} onChange={(valor) => cambiar('fechaColor', valor)} />
              <Numero etiqueta="Tamaño del icono" valor={formato.fechaIconoTamano} disabled={!writable} onChange={(valor) => cambiar('fechaIconoTamano', valor)} />
              <div className="formato-marcas">
                <Marca etiqueta="Negrita" valor={formato.fechaNegrita} disabled={!writable} onChange={(valor) => cambiar('fechaNegrita', valor)} />
              </div>
            </Grupo>
            {[...formato.tablas]
              .sort((a, b) => TIPOS.findIndex((item) => item.id === a.tipo) - TIPOS.findIndex((item) => item.id === b.tipo))
              .map((tabla) => {
                const leyenda = tabla.tipo === 'POSICIONES'
                  ? 'Tabla de posiciones'
                  : tabla.tipo === 'HORARIOS'
                    ? 'Tabla de horarios'
                    : `Tabla de ${(TIPOS.find((item) => item.id === tabla.tipo)?.etiqueta ?? tabla.tipo).toLowerCase()}`
                return (
                  <Grupo key={tabla.tipo} titulo={leyenda} plano>
                    <label className="formato-texto">
                      Fondo publicación
                      <input
                        value={tabla.fondoPublicacion}
                        disabled={!writable}
                        onChange={(event) => cambiarTabla(tabla.tipo, 'fondoPublicacion', event.target.value)}
                      />
                    </label>
                    <p className="formato-nota">Si queda vacío, toma la imagen plantilla de la liga. La imagen de esta tabla se actualiza al cambiarlo.</p>
                    <div className="formato-secciones">
                      <Grupo titulo="Título" abierto>
                        <label className="formato-texto">
                          Texto
                          <input
                            value={tabla.titulo}
                            disabled={!writable}
                            onChange={(event) => cambiarTabla(tabla.tipo, 'titulo', event.target.value)}
                          />
                        </label>
                        <Numero etiqueta="Tamaño" valor={tabla.tituloTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'tituloTamano', valor)} />
                        <Numero etiqueta="Posición X" valor={tabla.tituloX} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'tituloX', valor)} />
                        <Numero etiqueta="Posición Y" valor={tabla.tituloY} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'tituloY', valor)} />
                      </Grupo>
                      <Grupo titulo="Subtítulo" abierto>
                        <Numero etiqueta="Tamaño" valor={tabla.subtituloTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'subtituloTamano', valor)} />
                        <Numero etiqueta="Posición X" valor={tabla.subtituloX} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'subtituloX', valor)} />
                        <Numero etiqueta="Posición Y" valor={tabla.subtituloY} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'subtituloY', valor)} />
                      </Grupo>
                      <Grupo titulo="Fecha" abierto>
                        <Numero etiqueta="Tamaño del texto" valor={tabla.fechaTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'fechaTamano', valor)} />
                        <Numero etiqueta="Posición X" valor={tabla.fechaX} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'fechaX', valor)} />
                        <Numero etiqueta="Posición Y" valor={tabla.fechaY} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'fechaY', valor)} />
                      </Grupo>
                      <Grupo titulo="Nombre de lugar" abierto>
                        <Numero etiqueta="Tamaño" valor={tabla.lugarTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'lugarTamano', valor)} />
                        <Numero etiqueta="Posición Y" valor={tabla.lugarY} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'lugarY', valor)} />
                      </Grupo>
                      <Grupo titulo="Primera columna" abierto>
                        <Marca
                          etiqueta="Agrupar filas con el mismo valor"
                          valor={tabla.agruparPrimeraColumna}
                          disabled={!writable}
                          onChange={(valor) => cambiarTabla(tabla.tipo, 'agruparPrimeraColumna', valor)}
                        />
                      </Grupo>
                      <Grupo titulo="Posición de la tabla" abierto>
                        <Numero etiqueta="X" valor={tabla.tablaX} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'tablaX', valor)} />
                        <Numero etiqueta="Y" valor={tabla.tablaY} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'tablaY', valor)} />
                        <Numero etiqueta="Ancho" valor={tabla.tablaAncho} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'tablaAncho', valor)} />
                      </Grupo>
                      <Grupo titulo="Borde" abierto>
                        <Numero etiqueta="Grosor" valor={tabla.bordeGrosor} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'bordeGrosor', valor)} />
                        <ColorCampo etiqueta="Color" valor={tabla.bordeColor} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'bordeColor', valor)} />
                      </Grupo>
                      <Grupo titulo="Cabecera" abierto>
                        <Numero etiqueta="Alto" valor={tabla.cabeceraAlto} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'cabeceraAlto', valor)} />
                        <Numero etiqueta="Tamaño del texto" valor={tabla.cabeceraTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'cabeceraTamano', valor)} />
                        <ColorCampo etiqueta="Fondo" valor={tabla.cabeceraFondo} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'cabeceraFondo', valor)} />
                        <ColorCampo etiqueta="Color del texto" valor={tabla.cabeceraColor} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'cabeceraColor', valor)} />
                      </Grupo>
                      <Grupo titulo="Filas" abierto>
                        <Numero etiqueta="Alto" valor={tabla.filaAlto} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'filaAlto', valor)} />
                        <Numero etiqueta="Tamaño del texto" valor={tabla.filaTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'filaTamano', valor)} />
                        <ColorCampo etiqueta="Fondo" valor={tabla.filaFondo} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'filaFondo', valor)} />
                        <ColorCampo etiqueta="Color del texto" valor={tabla.filaColor} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'filaColor', valor)} />
                      </Grupo>
                      <Grupo titulo="Número" abierto>
                        <Numero etiqueta="Tamaño" valor={tabla.numeroTamano} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'numeroTamano', valor)} />
                        <ColorCampo etiqueta="Color" valor={tabla.numeroColor} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'numeroColor', valor)} />
                      </Grupo>
                      <Grupo titulo="Columna destacada" abierto>
                        <ColorCampo etiqueta="Fondo" valor={tabla.destacadaFondo} disabled={!writable} onChange={(valor) => cambiarTabla(tabla.tipo, 'destacadaFondo', valor)} />
                      </Grupo>
                    </div>
                  </Grupo>
                )
              })}
          </div>
          {writable && (
            <div className="actions">
              <button type="submit" disabled={guardando}>Guardar formato</button>
            </div>
          )}
          {error && <p className="down">{error}</p>}
        </form>
      )}
      {formato && (
        <form className="filtros" onSubmit={(event) => event.preventDefault()}>
          <label>
            Publicación
            <select value={tipo} onChange={(event) => setTipo(event.target.value as (typeof TIPOS)[number]['id'])}>
              {TIPOS.map((item) => <option key={item.id} value={item.id}>{item.etiqueta}</option>)}
            </select>
          </label>
          <label>
            Categoría
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              {categorias.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </label>
          <label>
            Fecha
            <FechaCampo value={fecha} onChange={setFecha} ariaLabel="Fecha" />
          </label>
          {tipo === 'POSICIONES' && grupos.length > 1 && (
            <label>
              Grupo
              <select value={grupoId} onChange={(event) => setGrupoId(event.target.value)}>
                {grupos.map((grupo) => {
                  const id = grupo.id ?? grupo.nombre
                  return <option key={id} value={id}>{grupo.nombre}</option>
                })}
              </select>
            </label>
          )}
          <button type="button" onClick={descargar}>Descargar PNG</button>
        </form>
      )}
      {formato && tipo === 'POSICIONES' && (
        <div className="columnas-check">
          {COLUMNAS_TABLA.map((columna) => (
            <label className="check" key={columna.codigo}>
              <input
                type="checkbox"
                checked={marcadas.has(columna.codigo)}
                onChange={(event) => marcarColumna(columna.codigo, event.target.checked)}
              />
              {columna.etiqueta}
            </label>
          ))}
        </div>
      )}
      {formato && <canvas ref={lienzo} className="publicacion-lienzo" />}
    </section>
  )
}
