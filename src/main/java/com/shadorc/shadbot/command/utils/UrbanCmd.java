package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.urbandictionary.UrbanDefinition;
import com.shadorc.shadbot.api.urbandictionary.UrbanDictionaryResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.LoadingMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class UrbanCmd extends BaseCmd {

    public UrbanCmd() {
        super(CommandCategory.UTILS, List.of("urban"), "ud");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

        final String url = String.format("https://api.urbandictionary.com/v0/define?term=%s", NetUtils.encode(arg));
        return NetUtils.get(url, UrbanDictionaryResponse.class)
                .map(urbanDictionary -> {
                    if (urbanDictionary.getDefinitions().isEmpty()) {
                        return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No urban definitions found for `%s`",
                                context.getUsername(), arg));
                    }

                    final UrbanDefinition urbanDefinition = urbanDictionary.getDefinitions().get(0);

                    final String definition = StringUtils.abbreviate(urbanDefinition.getDefinition(), Embed.MAX_DESCRIPTION_LENGTH);
                    final String example = StringUtils.abbreviate(urbanDefinition.getExample(), Field.MAX_VALUE_LENGTH);

                    return loadingMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> {
                                embed.setAuthor(String.format("Urban Dictionary: %s", urbanDefinition.getWord()), urbanDefinition.getPermalink(), context.getAvatarUrl())
                                        .setThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
                                        .setDescription(definition);
                                if (!example.isBlank()) {
                                    embed.addField("Example", example, false);
                                }
                            }));
                })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show Urban Dictionary definition for a search.")
                .addArg("search", false)
                .build();
    }

}
