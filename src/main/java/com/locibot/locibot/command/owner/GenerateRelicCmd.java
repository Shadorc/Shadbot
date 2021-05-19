package com.locibot.locibot.command.owner;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.premium.PremiumCollection;
import com.locibot.locibot.database.premium.RelicType;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.StringUtil;
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
                .flatMap(relic -> context.createFollowupMessage(Emoji.CHECK_MARK, "%s relic generated: **%s**"
                        .formatted(StringUtil.capitalize(type.name()), relic.getId())));
    }

}
