/*
package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class BassBoostCmd extends BaseCmd {

    private static final int VALUE_MIN = 0;
    private static final int VALUE_MAX = 200;

    public BassBoostCmd() {
        super(CommandCategory.MUSIC, List.of("bass_boost"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return DatabaseManager.getUsers().getDBUser(context.getAuthorId())
                .map(DBUser::getAchievements)
                .filter(achievements -> achievements.contains(Achievement.VOTER))
                .switchIfEmpty(Mono.error(new CommandException(
                        String.format("You can use this command by contributing to Shadbot <%s> or by unlocking the " +
                                        "**%s** achievement (more information using `%sachievements`).",
                                Config.PATREON_URL, Achievement.VOTER.getTitle(), context.getPrefix()))))
                .flatMap(ignored -> {
                    final String arg = context.requireArg();

                    final Integer percentage = NumberUtils.toIntBetweenOrNull(arg, VALUE_MIN, VALUE_MAX);
                    if (percentage == null) {
                        return Mono.error(new CommandException(
                                String.format("Incorrect value. Must be between **%d** and **%d**.", VALUE_MIN, VALUE_MAX)));
                    }

                    context.requireGuildMusic()
                            .getTrackScheduler()
                            .bassBoost(percentage);

                    final String text;
                    if (percentage == 0) {
                        text = String.format(Emoji.CHECK_MARK + " Bass boost disabled by **%s**.", context.getUsername());
                    } else {
                        text = String.format(Emoji.CHECK_MARK + " Bass boost set to **%d%%** by **%s**.", percentage,
                                context.getUsername());
                    }

                    return context.getChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                            .then();
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Drop the bass.")
                .addArg("percentage",
                        String.format("must be between **%d%%** and **%d%%**.", VALUE_MIN, VALUE_MAX), false)
                .addField("Info", String.format("**%d%%** will disable the boost." +
                        "%nA value higher than **%d%%** will saturate the sound.", VALUE_MIN, VALUE_MAX), false)
                .build();
    }
}
*/
