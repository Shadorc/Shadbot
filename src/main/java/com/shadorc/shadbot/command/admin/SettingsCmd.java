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
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
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
                    .map(DBGuild::getSettings)
                    .flatMap(settings -> SettingsCmd.show(context, settings))
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

    private static Mono<Consumer<EmbedCreateSpec>> show(Context context, Settings settings) {
        return Flux.fromIterable(SettingManager.getInstance().getSettings().values())
                .distinct()
                .flatMap(setting -> setting.show(context, settings))
                .reduce("", (desc, text) -> desc + "\n" + text)
                .map(text -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Settings", null, context.getAvatarUrl())
                                .setDescription(text.isEmpty() ? "There is no custom settings for this server." : text)));
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
                        setting.getDescription(), false));

        return embed.build();
    }

}
