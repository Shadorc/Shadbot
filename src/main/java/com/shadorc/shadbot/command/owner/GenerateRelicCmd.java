package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.database.premium.PremiumCollection;
import com.shadorc.shadbot.database.premium.RelicType;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class GenerateRelicCmd extends BaseCmd {

    public GenerateRelicCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "generate_relic", "Generate a relic");
        this.addOption("type", "Relic type", true,
                ApplicationCommandOptionType.STRING, DiscordUtil.toOptions(RelicType.class));
    }

    @Override
    public Mono<?> execute(Context context) {
        final RelicType type = context.getOptionAsEnum(RelicType.class, "type").orElseThrow();

        return PremiumCollection.generateRelic(type)
                .flatMap(relic -> context.reply(Emoji.CHECK_MARK, "%s relic generated: **%s**"
                        .formatted(StringUtil.capitalize(type.name()), relic.getId())));
    }

}
