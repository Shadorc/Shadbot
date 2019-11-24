package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class GenerateRelicCmd extends BaseCmd {

    public GenerateRelicCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("generate_relic", "generate-relic", "generaterelic"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final RelicType type = Utils.parseEnum(RelicType.class, context.getArg().orElseThrow(),
                new CommandException(String.format("`%s` in not a valid type. %s",
                        arg, FormatUtils.options(RelicType.class))));

        final Relic relic = DatabaseManager.getPremium().generateRelic(type);
        LogUtils.info("Relic generated. Type: %s, ID: %s", relic.getType(), relic.getId());
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s relic generated: **%s**",
                        StringUtils.capitalize(type.toString()), relic.getId()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Generate a relic.")
                .addArg("type", FormatUtils.format(RelicType.class, "/"), false)
                .build();
    }
}
