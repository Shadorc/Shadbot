package com.shadorc.shadbot.command.admin;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.SettingManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class SettingsCmd extends BaseCmd {

    public SettingsCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("setting", "settings"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, Integer.MAX_VALUE);

        if ("show".equals(args.get(0))) {
            return DatabaseManager.getGuilds()
                    .getDBGuild(context.getGuildId())
                    .flatMap(dbGuild -> SettingsCmd.show(context, dbGuild))
                    .flatMap(embed -> context.getChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                    .then();
        }

        final BaseSetting setting = SettingManager.getInstance().getSetting(args.get(0));
        if (setting == null) {
            return Mono.error(new CommandException(String.format("Setting `%s` does not exist. Use `%shelp %s` " +
                    "to see all available settings.", args.get(0), context.getPrefix(), this.getName())));
        }

        final String arg = args.size() == 2 ? args.get(1) : null;
        if ("help".equals(arg)) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(setting.getHelp(context), channel))
                    .then();
        }

        try {
            return setting.execute(context)
                    .then(DatabaseManager.getUsers().getDBUser(context.getAuthorId()))
                    .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.ENGINEER))
                    .then();
        } catch (final MissingArgumentException err) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            Emoji.WHITE_FLAG + " Some arguments are missing, here is the help for this setting.",
                            setting.getHelp(context), channel))
                    .then();
        }
    }

    private static Mono<Consumer<EmbedCreateSpec>> show(Context context, DBGuild dbGuild) {
        final StringBuilder settingsStr = new StringBuilder();

        if (!dbGuild.getSettings().getPrefix().equals(Config.DEFAULT_PREFIX)) {
            settingsStr.append(String.format("**Prefix:** %s", context.getPrefix()));
        }

        if (dbGuild.getSettings().getDefaultVol() != Config.DEFAULT_VOLUME) {
            settingsStr.append(String.format("%n**Default volume:** %d%%", dbGuild.getSettings().getDefaultVol()));
        }

        if (!dbGuild.getSettings().getBlacklistedCmds().isEmpty()) {
            settingsStr.append(String.format("%n**Blacklisted commands:**%n\t%s",
                    String.join("\n\t", dbGuild.getSettings().getBlacklistedCmds())));
        }

        dbGuild.getSettings().getJoinMessage()
                .ifPresent(joinMessage -> settingsStr.append(String.format("%n**Join message:**%n%s", joinMessage)));
        dbGuild.getSettings().getLeaveMessage()
                .ifPresent(leaveMessage -> settingsStr.append(String.format("%n**Leave message:**%n%s", leaveMessage)));

        final Mono<Void> autoMessageChannelStr = Mono.justOrEmpty(dbGuild.getSettings().getMessageChannelId())
                .flatMap(context.getClient()::getChannelById)
                .map(Channel::getMention)
                .map(channel -> settingsStr.append(String.format("%n**Auto message channel:** %s", channel)))
                .then();

        final Mono<Void> allowedTextChannelsStr = Flux.fromIterable(dbGuild.getSettings().getAllowedTextChannelIds())
                .flatMap(context.getClient()::getChannelById)
                .map(Channel::getMention)
                .collectList()
                .filter(channels -> !channels.isEmpty())
                .map(channels -> settingsStr.append(String.format("%n**Allowed text channels:**%n\t%s",
                        String.join("\n\t", channels))))
                .then();

        final Mono<Void> allowedVoiceChannelsStr = Flux.fromIterable(dbGuild.getSettings().getAllowedVoiceChannelIds())
                .flatMap(context.getClient()::getChannelById)
                .map(Channel::getMention)
                .collectList()
                .filter(channels -> !channels.isEmpty())
                .map(channels -> settingsStr.append(String.format("%n**Allowed voice channels:**%n\t%s",
                        String.join("\n\t", channels))))
                .then();

        final Mono<Void> autoRolesStr = Flux.fromIterable(dbGuild.getSettings().getAutoRoleIds())
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .map(Role::getMention)
                .collectList()
                .filter(roles -> !roles.isEmpty())
                .map(roles -> settingsStr.append(String.format("%n**Auto-roles:**%n\t%s",
                        String.join("\n\t", roles))))
                .then();

        final Mono<Void> allowedRolesStr = Flux.fromIterable(dbGuild.getSettings().getAllowedRoleIds())
                .flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
                .map(Role::getMention)
                .collectList()
                .filter(roles -> !roles.isEmpty())
                .map(roles -> settingsStr.append(String.format("%n**Allowed roles:**%n\t%s",
                        String.join("\n\t", roles))))
                .then();

        return autoMessageChannelStr
                .then(allowedTextChannelsStr)
                .then(allowedVoiceChannelsStr)
                .then(autoRolesStr)
                .then(allowedRolesStr)
                .thenReturn(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Settings", null, context.getAvatarUrl())
                                .setDescription(
                                        settingsStr.length() == 0 ? "There is no custom settings for this server." :
                                                settingsStr.toString())));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        final HelpBuilder embed = CommandHelpBuilder.create(this, context)
                .setThumbnail("https://i.imgur.com/QA2PUjM.png")
                .setDescription("Change Shadbot's settings for this server.")
                .addArg("name", false)
                .addArg("args", false)
                .addField("Additional Help", String.format("`%s%s <name> help`",
                        context.getPrefix(), this.getName()), false)
                .addField("Current settings", String.format("`%s%s show`",
                        context.getPrefix(), this.getName()), false);

        SettingManager.getInstance().getSettings()
                .values()
                .stream()
                .distinct()
                .forEach(setting -> embed.addField(String.format("Name: %s", setting.getName()),
                        setting.getDescription(),
                        false));

        return embed.build();
    }

}
