package com.shadorc.shadbot.service;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.StringUtil;
import io.sentry.Sentry;
import reactor.util.Logger;

public class SentryService implements Service {

    private static final Logger LOGGER = LogUtil.getLogger(SentryService.class, LogUtil.Category.SERVICE);

    private final String sentryDsn;

    public SentryService() {
        this.sentryDsn = CredentialManager.get(Credential.SENTRY_DSN);
    }

    @Override
    public boolean isEnabled() {
        return !Config.IS_SNAPSHOT && !StringUtil.isBlank(this.sentryDsn);
    }

    @Override
    public void start() {
        LOGGER.info("Sarting Sentry service");
        Sentry.init(options -> {
            options.setDsn(this.sentryDsn);
            options.setRelease(Config.VERSION);
            // Ignore events coming from lavaplayer
            options.setBeforeSend(
                    (sentryEvent, obj) -> sentryEvent.getLogger().startsWith("com.sedmelluq") ? null : sentryEvent);
        });
    }

    @Override
    public void stop() {
        LOGGER.info("Stopping Sentry service");
        Sentry.close();
    }
}
