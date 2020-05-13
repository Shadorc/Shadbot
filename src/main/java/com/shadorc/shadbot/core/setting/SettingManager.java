package com.shadorc.shadbot.core.setting;

import com.shadorc.shadbot.command.admin.setting.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class SettingManager {

    private static SettingManager instance;

    static {
        SettingManager.instance = new SettingManager();
    }

    private final Map<String, BaseSetting> settingsMap;

    private SettingManager() {
        this.settingsMap = this.initialize(
                new AllowedChannelsSetting(), new AllowedRolesSetting(), new AutoMessagesSetting(),
                new AutoRolesSetting(), new BlacklistSettingCmd(), new NSFWSetting(), new PrefixSetting(),
                new VolumeSetting());
    }

    private Map<String, BaseSetting> initialize(BaseSetting... settings) {
        final Map<String, BaseSetting> map = new LinkedHashMap<>();
        for (final BaseSetting setting : settings) {
            for (final String name : setting.getNames()) {
                if (map.putIfAbsent(name, setting) != null) {
                    DEFAULT_LOGGER.error("Setting name collision between {} and {}",
                            name, map.get(name).getClass().getSimpleName());
                }
            }
        }
        DEFAULT_LOGGER.info("{} settings initialized", settings.length);
        return Collections.unmodifiableMap(map);
    }

    public Map<String, BaseSetting> getSettings() {
        return this.settingsMap;
    }

    public BaseSetting getSetting(String name) {
        return this.settingsMap.get(name);
    }

    public static SettingManager getInstance() {
        return SettingManager.instance;
    }
}
