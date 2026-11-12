package com.away.core.competition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ResultadoViews {

    private ResultadoViews() {
    }

    public static class ResultadoFechaView {
        private final LocalDate fecha;

        public ResultadoFechaView(LocalDate fecha) {
            this.fecha = fecha;
        }

        public LocalDate getFecha() {
            return fecha;
        }
    }

    public static class ResultadoGuardarRequest {
        private Integer golesLocal;
        private Integer golesVisita;

        public Integer getGolesLocal() {
            return golesLocal;
        }

        public void setGolesLocal(Integer golesLocal) {
            this.golesLocal = golesLocal;
        }

        public Integer getGolesVisita() {
            return golesVisita;
        }

        public void setGolesVisita(Integer golesVisita) {
            this.golesVisita = golesVisita;
        }
    }

    public static class ResultadoPartidoView {
        private final UUID id;
        private final String categoryName;
        private final String jornadaNombre;
        private final String canchaNombre;
        private final LocalDate fecha;
        private final String hora;
        private final String local;
        private final String visita;
        private final Integer golesLocal;
        private final Integer golesVisita;
        private final boolean equiposDefinidos;
        private final int orden;
        private final BigDecimal valorCobrado;
        private final boolean cerrada;

        ResultadoPartidoView(UUID id, String categoryName, String jornadaNombre, String canchaNombre,
                             LocalDate fecha, String hora, String local, String visita,
                             Integer golesLocal, Integer golesVisita, boolean equiposDefinidos, int orden,
                             BigDecimal valorCobrado, boolean cerrada) {
            this.id = id;
            this.categoryName = categoryName;
            this.jornadaNombre = jornadaNombre;
            this.canchaNombre = canchaNombre;
            this.fecha = fecha;
            this.hora = hora;
            this.local = local;
            this.visita = visita;
            this.golesLocal = golesLocal;
            this.golesVisita = golesVisita;
            this.equiposDefinidos = equiposDefinidos;
            this.orden = orden;
            this.valorCobrado = valorCobrado;
            this.cerrada = cerrada;
        }

        public UUID getId() {
            return id;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getJornadaNombre() {
            return jornadaNombre;
        }

        public String getCanchaNombre() {
            return canchaNombre;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String getHora() {
            return hora;
        }

        public String getLocal() {
            return local;
        }

        public String getVisita() {
            return visita;
        }

        public Integer getGolesLocal() {
            return golesLocal;
        }

        public Integer getGolesVisita() {
            return golesVisita;
        }

        public boolean isEquiposDefinidos() {
            return equiposDefinidos;
        }

        public int getOrden() {
            return orden;
        }

        public BigDecimal getValorCobrado() {
            return valorCobrado;
        }

        public boolean isCerrada() {
            return cerrada;
        }
    }

    public static class ResultadoPanelView {
        private final LocalDate hoy;
        private final List<ResultadoPartidoView> partidos;
        private final boolean cerrada;

        public ResultadoPanelView(LocalDate hoy, List<ResultadoPartidoView> partidos, boolean cerrada) {
            this.hoy = hoy;
            this.partidos = partidos;
            this.cerrada = cerrada;
        }

        public LocalDate getHoy() {
            return hoy;
        }

        public List<ResultadoPartidoView> getPartidos() {
            return partidos;
        }

        public boolean isCerrada() {
            return cerrada;
        }
    }

    public static class CierreView {
        private final boolean cerrada;

        public CierreView(boolean cerrada) {
            this.cerrada = cerrada;
        }

        public boolean isCerrada() {
            return cerrada;
        }
    }

    public static class ArbitroAsignadoRequest {
        private UUID arbitroId;
        private boolean pito;

        public UUID getArbitroId() {
            return arbitroId;
        }

        public void setArbitroId(UUID arbitroId) {
            this.arbitroId = arbitroId;
        }

        public boolean isPito() {
            return pito;
        }

        public void setPito(boolean pito) {
            this.pito = pito;
        }
    }

    public static class GolJugadorRequest {
        private UUID jugadorId;
        private Integer cantidad;

        public UUID getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(UUID jugadorId) {
            this.jugadorId = jugadorId;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }

    public static class TarjetaJugadorRequest {
        private UUID jugadorId;
        private Integer amarillas;
        private Integer rojas;
        private String observacion;

        public UUID getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(UUID jugadorId) {
            this.jugadorId = jugadorId;
        }

        public Integer getAmarillas() {
            return amarillas;
        }

        public void setAmarillas(Integer amarillas) {
            this.amarillas = amarillas;
        }

        public Integer getRojas() {
            return rojas;
        }

        public void setRojas(Integer rojas) {
            this.rojas = rojas;
        }

        public String getObservacion() {
            return observacion;
        }

        public void setObservacion(String observacion) {
            this.observacion = observacion;
        }
    }

    public static class DetalleGuardarRequest {
        private List<ArbitroAsignadoRequest> arbitros;
        private List<GolJugadorRequest> goles;
        private List<TarjetaJugadorRequest> tarjetas;
        private List<SancionPartidoRequest> sanciones;

        public List<ArbitroAsignadoRequest> getArbitros() {
            return arbitros;
        }

        public void setArbitros(List<ArbitroAsignadoRequest> arbitros) {
            this.arbitros = arbitros;
        }

        public List<GolJugadorRequest> getGoles() {
            return goles;
        }

        public void setGoles(List<GolJugadorRequest> goles) {
            this.goles = goles;
        }

        public List<TarjetaJugadorRequest> getTarjetas() {
            return tarjetas;
        }

        public void setTarjetas(List<TarjetaJugadorRequest> tarjetas) {
            this.tarjetas = tarjetas;
        }

        public List<SancionPartidoRequest> getSanciones() {
            return sanciones;
        }

        public void setSanciones(List<SancionPartidoRequest> sanciones) {
            this.sanciones = sanciones;
        }
    }

    public static class SancionPartidoRequest {
        private UUID jugadorId;
        private String observacion;
        private List<UUID> sancionIds;

        public UUID getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(UUID jugadorId) {
            this.jugadorId = jugadorId;
        }

        public String getObservacion() {
            return observacion;
        }

        public void setObservacion(String observacion) {
            this.observacion = observacion;
        }

        public List<UUID> getSancionIds() {
            return sancionIds;
        }

        public void setSancionIds(List<UUID> sancionIds) {
            this.sancionIds = sancionIds;
        }
    }

    public static class SancionOpcionView {
        private final UUID id;
        private final String descripcion;
        private final BigDecimal costo;
        private final String estado;

        public SancionOpcionView(UUID id, String descripcion, BigDecimal costo, String estado) {
            this.id = id;
            this.descripcion = descripcion;
            this.costo = costo;
            this.estado = estado;
        }

        public UUID getId() {
            return id;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public BigDecimal getCosto() {
            return costo;
        }

        public String getEstado() {
            return estado;
        }
    }

    public static class SancionCargadaView {
        private final UUID jugadorId;
        private final String observacion;
        private final List<UUID> sancionIds;

        public SancionCargadaView(UUID jugadorId, String observacion, List<UUID> sancionIds) {
            this.jugadorId = jugadorId;
            this.observacion = observacion;
            this.sancionIds = sancionIds;
        }

        public UUID getJugadorId() {
            return jugadorId;
        }

        public String getObservacion() {
            return observacion;
        }

        public List<UUID> getSancionIds() {
            return sancionIds;
        }
    }

    public static class ArbitroOpcionView {
        private final UUID id;
        private final String nombre;
        private final boolean hasFoto;

        public ArbitroOpcionView(UUID id, String nombre, boolean hasFoto) {
            this.id = id;
            this.nombre = nombre;
            this.hasFoto = hasFoto;
        }

        public UUID getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public boolean isHasFoto() {
            return hasFoto;
        }
    }

    public static class ArbitroAsignadoView {
        private final UUID arbitroId;
        private final boolean pito;

        public ArbitroAsignadoView(UUID arbitroId, boolean pito) {
            this.arbitroId = arbitroId;
            this.pito = pito;
        }

        public UUID getArbitroId() {
            return arbitroId;
        }

        public boolean isPito() {
            return pito;
        }
    }

    public static class GolCargadoView {
        private final UUID jugadorId;
        private final int cantidad;

        public GolCargadoView(UUID jugadorId, int cantidad) {
            this.jugadorId = jugadorId;
            this.cantidad = cantidad;
        }

        public UUID getJugadorId() {
            return jugadorId;
        }

        public int getCantidad() {
            return cantidad;
        }
    }

    public static class TarjetaCargadaView {
        private final UUID jugadorId;
        private final Integer amarillas;
        private final Integer rojas;
        private final String observacion;

        public TarjetaCargadaView(UUID jugadorId, Integer amarillas, Integer rojas, String observacion) {
            this.jugadorId = jugadorId;
            this.amarillas = amarillas;
            this.rojas = rojas;
            this.observacion = observacion;
        }

        public UUID getJugadorId() {
            return jugadorId;
        }

        public Integer getAmarillas() {
            return amarillas;
        }

        public Integer getRojas() {
            return rojas;
        }

        public String getObservacion() {
            return observacion;
        }
    }

    public static class EquipoOpcionView {
        private final UUID id;
        private final String nombre;

        public EquipoOpcionView(UUID id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public UUID getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }
    }

    public static class DetalleView {
        private final String local;
        private final String visita;
        private final EquipoOpcionView equipoLocal;
        private final EquipoOpcionView equipoVisita;
        private final boolean editable;
        private final List<ArbitroOpcionView> arbitros;
        private final List<ArbitroAsignadoView> asignados;
        private final List<GolCargadoView> goles;
        private final List<TarjetaCargadaView> tarjetas;
        private final List<SancionOpcionView> sancionesDisponibles;
        private final List<SancionCargadaView> sanciones;

        public DetalleView(String local, String visita, EquipoOpcionView equipoLocal, EquipoOpcionView equipoVisita,
                           boolean editable, List<ArbitroOpcionView> arbitros, List<ArbitroAsignadoView> asignados,
                           List<GolCargadoView> goles, List<TarjetaCargadaView> tarjetas,
                           List<SancionOpcionView> sancionesDisponibles, List<SancionCargadaView> sanciones) {
            this.local = local;
            this.visita = visita;
            this.equipoLocal = equipoLocal;
            this.equipoVisita = equipoVisita;
            this.editable = editable;
            this.arbitros = arbitros;
            this.asignados = asignados;
            this.goles = goles;
            this.tarjetas = tarjetas;
            this.sancionesDisponibles = sancionesDisponibles;
            this.sanciones = sanciones;
        }

        public String getLocal() {
            return local;
        }

        public String getVisita() {
            return visita;
        }

        public EquipoOpcionView getEquipoLocal() {
            return equipoLocal;
        }

        public EquipoOpcionView getEquipoVisita() {
            return equipoVisita;
        }

        public boolean isEditable() {
            return editable;
        }

        public List<ArbitroOpcionView> getArbitros() {
            return arbitros;
        }

        public List<ArbitroAsignadoView> getAsignados() {
            return asignados;
        }

        public List<GolCargadoView> getGoles() {
            return goles;
        }

        public List<TarjetaCargadaView> getTarjetas() {
            return tarjetas;
        }

        public List<SancionOpcionView> getSancionesDisponibles() {
            return sancionesDisponibles;
        }

        public List<SancionCargadaView> getSanciones() {
            return sanciones;
        }
    }

    public static class JugadorPartidoView {
        private final UUID id;
        private final int numero;
        private final String alias;
        private final boolean hasFoto;
        private final int rotacion;
        private final boolean suspendido;

        public JugadorPartidoView(UUID id, int numero, String alias, boolean hasFoto, int rotacion, boolean suspendido) {
            this.id = id;
            this.numero = numero;
            this.alias = alias;
            this.hasFoto = hasFoto;
            this.rotacion = rotacion;
            this.suspendido = suspendido;
        }

        public UUID getId() {
            return id;
        }

        public int getNumero() {
            return numero;
        }

        public String getAlias() {
            return alias;
        }

        public boolean isHasFoto() {
            return hasFoto;
        }

        public int getRotacion() {
            return rotacion;
        }

        public boolean isSuspendido() {
            return suspendido;
        }
    }
}
