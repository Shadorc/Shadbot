package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.premium.PremiumCollection;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class GenerateRelicCmd extends BaseCmd {

    public GenerateRelicCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "generate_relic", "Generate a relic");

        this.addOption("type", FormatUtil.format(RelicType.class, "/"), false, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String typeStr = context.getOptionAsString("type").orElseThrow();

        final RelicType type = EnumUtil.parseEnum(RelicType.class, typeStr,
                new CommandException(String.format("`%s` in not a valid type. %s",
                        typeStr, FormatUtil.options(RelicType.class))));

        return PremiumCollection.generateRelic(type)
                .flatMap(relic -> context.createFollowupMessage(Emoji.CHECK_MARK + " %s relic generated: **%s**",
                        StringUtil.capitalize(type.toString()), relic.getId()));
    }

}
