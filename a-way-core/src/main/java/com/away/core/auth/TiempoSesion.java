package com.away.core.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class TiempoSesion implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private final int minutos;

    public TiempoSesion(@Value("${app.session.timeout-minutes:45}") int minutos) {
        this.minutos = minutos;
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.addInitializers(servletContext -> servletContext.setSessionTimeout(minutos));
    }
}
