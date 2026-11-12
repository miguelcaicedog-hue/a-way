package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SuspensionesService {

    static final String JORNADA_SIGUIENTE = "NEXT_MATCHDAY";
    static final String PRIMERA_FECHA = "FIRST_MATCHDAY_OF_DAY";

    private final LeagueRepository leagues;
    private final LeagueSettingsRepository ajustes;
    private final ReglaSuspensionRepository reglas;
    private final AppUserRepository users;

    public SuspensionesService(LeagueRepository leagues, LeagueSettingsRepository ajustes,
                               ReglaSuspensionRepository reglas, AppUserRepository users) {
        this.leagues = leagues;
        this.ajustes = ajustes;
        this.reglas = reglas;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public SuspensionesViews.ReglaView ver(UUID leagueId) {
        League league = league(leagueId);
        assertRead(league.getId());
        ReglaSuspension regla = reglas.findByIdLigaAndEstado(leagueId, "ACTIVO").orElse(null);
        return vista(leagueId, regla, momento(leagueId));
    }

    @Transactional
    public SuspensionesViews.ReglaView guardar(SuspensionesViews.GuardarRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        String momento = momento(request.getMomento());
        String amarillasMomento = momento(request.getAmarillasMomento());
        String rojasMomento = momento(request.getRojasMomento());
        ReglaSuspension regla = reglas.findByIdLigaAndEstado(league.getId(), "ACTIVO").orElse(null);
        boolean nueva = regla == null;
        if (nueva) {
            regla = new ReglaSuspension();
            regla.setId(UUID.randomUUID());
            regla.setIdLiga(league.getId());
            regla.setEstado("ACTIVO");
        }
        regla.setRojaJornadas(request.getRojaJornadas());
        regla.setAmarillasAcumuladas(request.getAmarillasAcumuladas());
        regla.setAmarillasJornadas(request.getAmarillasJornadas());
        regla.setAmarillasMomento(amarillasMomento);
        regla.setRojasAcumuladas(request.getRojasAcumuladas());
        regla.setRojasJornadas(request.getRojasJornadas());
        regla.setRojasMomento(rojasMomento);
        regla.setFaltaMediaJornadas(request.getFaltaMediaJornadas());
        auditar(regla, usuario(), nueva);
        reglas.save(regla);
        LeagueSettings ajuste = ajustes.findById(league.getId()).orElseGet(() -> {
            LeagueSettings creado = new LeagueSettings();
            creado.setLeagueId(league.getId());
            creado.setFairPlayYellowPoints(1);
            creado.setFairPlayRedPoints(3);
            return creado;
        });
        ajuste.setSuspensionTiming(momento);
        ajustes.save(ajuste);
        return vista(league.getId(), regla, momento);
    }

    @Transactional
    public void crearDefecto(UUID leagueId) {
        if (reglas.findByIdLigaAndEstado(leagueId, "ACTIVO").isPresent()) {
            return;
        }
        ReglaSuspension regla = new ReglaSuspension();
        regla.setId(UUID.randomUUID());
        regla.setIdLiga(leagueId);
        regla.setEstado("ACTIVO");
        regla.setRojaJornadas(1);
        regla.setAmarillasAcumuladas(3);
        regla.setAmarillasJornadas(1);
        regla.setAmarillasMomento(JORNADA_SIGUIENTE);
        regla.setRojasAcumuladas(2);
        regla.setRojasJornadas(2);
        regla.setRojasMomento(JORNADA_SIGUIENTE);
        regla.setFaltaMediaJornadas(3);
        auditar(regla, usuario(), true);
        reglas.save(regla);
    }

    private SuspensionesViews.ReglaView vista(UUID leagueId, ReglaSuspension regla, String momento) {
        if (regla == null) {
            return new SuspensionesViews.ReglaView(leagueId, momento, 1, 3, 1, JORNADA_SIGUIENTE, 2, 2, JORNADA_SIGUIENTE, 3);
        }
        return new SuspensionesViews.ReglaView(
                leagueId,
                momento,
                regla.getRojaJornadas(),
                regla.getAmarillasAcumuladas(),
                regla.getAmarillasJornadas(),
                regla.getAmarillasMomento(),
                regla.getRojasAcumuladas(),
                regla.getRojasJornadas(),
                regla.getRojasMomento(),
                regla.getFaltaMediaJornadas());
    }

    private String momento(UUID leagueId) {
        return ajustes.findById(leagueId).map(LeagueSettings::getSuspensionTiming).orElse(JORNADA_SIGUIENTE);
    }

    private String momento(String valor) {
        if (JORNADA_SIGUIENTE.equals(valor) || PRIMERA_FECHA.equals(valor)) {
            return valor;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El momento de la suspensión no es válido");
    }

    private void auditar(ReglaSuspension regla, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            regla.setFechaCreacion(ahora);
            regla.setUsuarioCreacion(usuario);
        }
        regla.setFechaModificacion(ahora);
        regla.setUsuarioModificacion(usuario);
    }

    private League league(UUID id) {
        return leagues.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AppUser actual() {
        return users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void assertRead(UUID leagueId) {
        AppUser user = actual();
        boolean miembro = user.isMaster() || user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
        if (!miembro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private void assertWrite(UUID leagueId) {
        AppUser user = actual();
        if (user.isMaster()) {
            return;
        }
        boolean admin = user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId)
                        && "ADMINISTRATIVO".equals(membership.getRole()));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta liga");
        }
    }
}
