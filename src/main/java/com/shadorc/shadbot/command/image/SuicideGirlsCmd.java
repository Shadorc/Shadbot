package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.html.suicidegirl.SuicideGirl;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class SuicideGirlsCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.suicidegirls.com/photos/sg/recent/all/";

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, List.of("suicide_girls", "suicide-girls", "suicidegirls"), "sg");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(
                String.format(Emoji.HOURGLASS + " (**%s**) Loading Suicide Girl picture...", context.getUsername()))
                .send()
                .then(context.isChannelNsfw())
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return Mono.just(updatableMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix())));
                    }

                    return this.getRandomSuicideGirl()
                            .map(girl -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                                    .andThen(embed -> embed.setAuthor("SuicideGirls", girl.getUrl(), context.getAvatarUrl())
                                            .setDescription(String.format("Name: **%s**", girl.getName()))
                                            .setImage(girl.getImageUrl()))));
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<SuicideGirl> getRandomSuicideGirl() {
        return NetUtils.get(HOME_URL)
                .map(Jsoup::parse)
                .map(SuicideGirl::new);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show a random Suicide Girl image.")
                .setSource("https://www.suicidegirls.com/")
                .build();
    }

}
