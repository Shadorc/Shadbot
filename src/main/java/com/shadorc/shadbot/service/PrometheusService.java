package com.shadorc.shadbot.service;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.StringUtil;
import io.prometheus.client.exporter.HTTPServer;
import reactor.util.Logger;

import java.io.IOException;

public class PrometheusService implements Service {

    private static final Logger LOGGER = LogUtil.getLogger(SentryService.class, LogUtil.Category.SERVICE);

    private final String port;

    private HTTPServer server;

    public PrometheusService() {
        this.port = CredentialManager.get(Credential.PROMETHEUS_PORT);
    }

    @Override
    public boolean isEnabled() {
        return !Config.IS_SNAPSHOT && !StringUtil.isBlank(this.port);
    }

    @Override
    public void start() {
        LOGGER.info("Starting Prometheus service on port {}", this.port);
        try {
            this.server = new HTTPServer(Integer.parseInt(this.port));
        } catch (final IOException err) {
            LOGGER.error("An error occurred while starting Prometheus service", err);
        }
    }

    @Override
    public void stop() {
        if (this.server != null) {
            LOGGER.info("Stopping Prometheus service");
            this.server.stop();
        }
    }
}
