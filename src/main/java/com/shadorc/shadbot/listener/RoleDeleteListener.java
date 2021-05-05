package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.Setting;
import com.shadorc.shadbot.database.DatabaseManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import reactor.core.publisher.Mono;

import java.util.Set;

public class RoleDeleteListener implements EventListener<RoleDeleteEvent> {

    @Override
    public Class<RoleDeleteEvent> getEventType() {
        return RoleDeleteEvent.class;
    }

    @Override
    public Mono<?> execute(RoleDeleteEvent event) {
        return DatabaseManager.getGuilds()
                .getDBGuild(event.getGuildId())
                .flatMap(dbGuild -> {
                    final Set<Snowflake> allowedRoleIds = dbGuild.getSettings().getAllowedRoleIds();
                    // If the role was an allowed role...
                    if (allowedRoleIds.remove(event.getRoleId())) {
                        // ...update settings to remove the deleted one
                        return dbGuild.updateSetting(Setting.ALLOWED_ROLES, allowedRoleIds);
                    }
                    return Mono.empty();
                });
    }

}
