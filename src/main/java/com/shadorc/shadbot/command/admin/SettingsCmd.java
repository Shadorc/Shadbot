package com.shadorc.shadbot.command.admin;

import com.shadorc.shadbot.command.admin.setting.*;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.database.DBGuild;
import com.shadorc.shadbot.db.database.DatabaseManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SettingsCmd extends BaseCmd {

    private final Map<Setting, BaseSetting> settingsMap;

    public SettingsCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("setting", "settings"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));

        this.settingsMap = new EnumMap<>(Setting.class);
        this.add(new AllowedChannelsSetting(), new AllowedRolesSetting(), new AutoMessageSetting(),
                new AutoRolesSetting(), new BlacklistSettingCmd(), new NSFWSetting(), new PrefixSetting(),
                new VolumeSetting());
    }

    private void add(BaseSetting... settings) {
        for (final BaseSetting setting : settings) {
            if (this.settingsMap.putIfAbsent(setting.getSetting(), setting) != null) {
                LogUtils.error(String.format("Command name collision between %s and %s",
                        setting.getName(), this.settingsMap.get(setting.getSetting()).getClass().getSimpleName()));
            }
        }
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, Integer.MAX_VALUE);

        if ("show".equals(args.get(0))) {
            return SettingsCmd.show(context)
                    .flatMap(embed -> context.getChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                    .then();
        }

        final Setting settingEnum = Utils.parseEnum(Setting.class, args.get(0));
        final BaseSetting setting = this.settingsMap.get(settingEnum);
        if (setting == null) {
            return Mono.error(new CommandException(String.format("Setting `%s` does not exist. Use `%shelp %s` to see all available settings.",
                    args.get(0), context.getPrefix(), this.getName())));
        }

        final String arg = args.size() == 2 ? args.get(1) : null;
        if ("help".equals(arg)) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(SettingsCmd.getHelp(context, setting), channel))
                    .then();
        }

        try {
            return setting.execute(context);
        } catch (final MissingArgumentException err) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            Emoji.WHITE_FLAG + " Some arguments are missing, here is the help for this setting.", SettingsCmd.getHelp(context, setting), channel))
                    .then();
        }
    }

    private static Mono<Consumer<EmbedCreateSpec>> show(Context context) {
        final DBGuild dbGuild = DatabaseManager.getInstance().getDBGuild(context.getGuildId());
        final StringBuilder settingsStr = new StringBuilder();

        if (!dbGuild.getPrefix().equals(Config.DEFAULT_PREFIX)) {
            settingsStr.append(String.format("**Prefix:** %s", context.getPrefix()));
        }

        if (dbGuild.getDefaultVol() != Config.DEFAULT_VOLUME) {
            settingsStr.append(String.format("%n**Default volume:** %d%%", dbGuild.getDefaultVol()));
        }

        if (!dbGuild.getBlacklistedCmd().isEmpty()) {
            settingsStr.append(String.format("%n**Blacklisted commands:**%n\t%s",
                    String.join("\n\t", dbGuild.getBlacklistedCmd())));
        }

        dbGuild.getJoinMessage()
                .ifPresent(joinMessage -> settingsStr.append(String.format("%n**Join message:**%n%s", joinMessage)));
        dbGuild.getLeaveMessage()
                .ifPresent(leaveMessage -> settingsStr.append(String.format("%n**Leave message:**%n%s", leaveMessage)));

        final Mono<Void> autoMessageChannelStr = Mono.justOrEmpty(dbGuild.getMessageChannelId())
                .map(Snowflake::of)
                .flatMap(context.getClient()::getChannelById)
                .map(Channel::getMention)
                .map(channel -> settingsStr.append(String.format("%n**Auto message channel:** %s", channel)))
                .then();

        final Mono<Void> allowedTextChannelsStr = Flux.fromIterable(dbGuild.getAllowedTextChannels())
                .map(Snowflake::of)
                .flatMap(context.getClient()::getChannelById)
                .map(Channel::getMention)
                .collectList()
                .filter(channels -> !channels.isEmpty())
                .map(channels -> settingsStr.append(String.format("%n**Allowed text channels:**%n\t%s", String.join("\n\t", channels))))
                .then();

        final Mono<Void> allowedVoiceChannelsStr = Flux.fromIterable(dbGuild.getAllowedVoiceChannels())
                .map(Snowflake::of)
                .flatMap(context.getClient()::getChannelById)
                .map(Channel::getMention)
                .collectList()
                .filter(channels -> !channels.isEmpty())
                .map(channels -> settingsStr.append(String.format("%n**Allowed voice channels:**%n\t%s", String.join("\n\t", channels))))
                .then();

        final Mono<Void> autoRolesStr = Flux.fromIterable(dbGuild.getAutoRoles())
                .map(Snowflake::of)
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .map(Role::getMention)
                .collectList()
                .filter(roles -> !roles.isEmpty())
                .map(roles -> settingsStr.append(String.format("%n**Auto-roles:**%n\t%s", String.join("\n\t", roles))))
                .then();

        final Mono<Void> allowedRolesStr = Flux.fromIterable(dbGuild.getAllowedRoles())
                .map(Snowflake::of)
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .map(Role::getMention)
                .collectList()
                .filter(roles -> !roles.isEmpty())
                .map(roles -> settingsStr.append(String.format("%n**Allowed roles:**%n\t%s", String.join("\n\t", roles))))
                .then();

        return autoMessageChannelStr
                .then(allowedTextChannelsStr)
                .then(allowedVoiceChannelsStr)
                .then(autoRolesStr)
                .then(allowedRolesStr)
                .thenReturn(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Settings", null, context.getAvatarUrl())
                                .setDescription(
                                        settingsStr.length() == 0 ? "There is no custom settings for this server." : settingsStr.toString())));
    }

    private static Consumer<EmbedCreateSpec> getHelp(Context context, BaseSetting setting) {
        return setting.getHelp(context)
                .andThen(embed -> embed.setAuthor(String.format("Help for setting: %s", setting.getName()), null, context.getAvatarUrl())
                        .setDescription(String.format("**%s**", setting.getDescription())));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        final HelpBuilder embed = new HelpBuilder(this, context)
                .setThumbnail("https://i.imgur.com/QA2PUjM.png")
                .setDescription("Change Shadbot's settings for this server.")
                .addArg("name", false)
                .addArg("args", false)
                .addField("Additional Help", String.format("`%s%s <name> help`",
                        context.getPrefix(), this.getName()), false)
                .addField("Current settings", String.format("`%s%s show`",
                        context.getPrefix(), this.getName()), false);

        this.settingsMap.values()
                .forEach(setting -> embed.addField(String.format("Name: %s", setting.getName()),
                        setting.getDescription(),
                        false));

        return embed.build();
    }

}
