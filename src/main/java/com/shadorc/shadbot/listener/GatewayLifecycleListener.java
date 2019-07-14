package com.shadorc.shadbot.listener;

import discord4j.core.event.domain.lifecycle.*;
import discord4j.gateway.retry.GatewayStateChange;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class GatewayLifecycleListener implements EventListener<GatewayLifecycleEvent> {

    private final Logger logger;
    private final AtomicBoolean isFullyReady;
    private volatile GatewayStateChange.State state;

    public GatewayLifecycleListener(Logger logger, AtomicBoolean isFullyReady) {
        this.logger = logger;
        this.isFullyReady = isFullyReady;
    }

    @Override
    public Class<GatewayLifecycleEvent> getEventType() {
        return GatewayLifecycleEvent.class;
    }

    @Override
    public Mono<Void> execute(GatewayLifecycleEvent event) {
        return Mono.fromRunnable(() -> {
            if (event instanceof ConnectEvent) {
                this.state = GatewayStateChange.State.CONNECTED;
            } else if (event instanceof ResumeEvent) {
                this.state = GatewayStateChange.State.CONNECTED;
            } else if (event instanceof ReadyEvent) {
                this.state = GatewayStateChange.State.CONNECTED;
            } else if (event instanceof DisconnectEvent) {
                this.state = GatewayStateChange.State.DISCONNECTED;
            } else if (event instanceof ReconnectStartEvent) {
                this.state = GatewayStateChange.State.RETRY_STARTED;
            } else if (event instanceof ReconnectFailEvent) {
                this.state = GatewayStateChange.State.RETRY_FAILED;
            } else if (event instanceof ReconnectEvent) {
                this.state = GatewayStateChange.State.RETRY_SUCCEEDED;
            }

            this.isFullyReady.set(this.state == GatewayStateChange.State.RETRY_SUCCEEDED);

            this.logger.info("New event: {} / fully ready: {}.",
                    event.getClass().getSimpleName(), this.isFullyReady.get());
        });
    }

}
