package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class VoteCmd extends BaseCmd {

    public VoteCmd() {
        super(CommandCategory.INFO, List.of("vote"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final Consumer<EmbedCreateSpec> embedConsumer = ShadbotUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor("Vote on top.gg", Config.TOP_GG_URL, context.getAvatarUrl())
                        .setThumbnail("https://i.imgur.com/4Rf7SlR.png")
                        .setDescription(String.format("If you like me, you can vote for me on **top.gg**!" +
                                        "%n%s" +
                                        "%nYou will unlock the **%s** achievement and the `%sbass_boost` command!",
                                Config.TOP_GG_URL, Achievement.VOTER.getTitle(), context.getPrefix())));

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("If you like me, vote for me on top.gg!")
                .build();
    }
}
