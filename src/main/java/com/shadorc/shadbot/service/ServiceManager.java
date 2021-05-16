package com.shadorc.shadbot.service;

import java.util.ArrayList;
import java.util.List;

public class ServiceManager {

    private final List<Service> services;

    public ServiceManager() {
        this.services = new ArrayList<>(4);
        this.services.add(new SentryService());
        this.services.add(new PrometheusService());
        this.services.add(new TopGgWebhookService());
    }

    public void addService(Service service) {
        this.services.add(service);
    }

    public void start() {
        this.services.stream()
                .filter(Service::isEnabled)
                .forEach(Service::start);
    }

    public void stop() {
        this.services.stream()
                .filter(Service::isEnabled)
                .forEach(Service::stop);
    }

}
