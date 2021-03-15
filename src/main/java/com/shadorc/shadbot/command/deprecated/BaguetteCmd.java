package com.shadorc.shadbot.command.deprecated;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class BaguetteCmd extends BaseCmd {

    public BaguetteCmd() {
        super(CommandCategory.HIDDEN, "baguette", "This command doesn't exist");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.reply(ShadbotUtil.getDefaultEmbed(
                embed -> embed.setImage("https://i.imgur.com/2Ean5iI.jpg")));
    }

    // Essential part of Shadbot (Thanks to @Bluerin)
    public static String howToDoAChocolateCake() { // NO_UCD (unused code)
        final String meal = "50g farine";
        final String chocolate = "200g chocolat";
        final String eggs = "3 oeufs";
        final String sugar = "100g sucre";
        final String butter = "100g beurre";
        return "Mélanger " + meal + " " + sugar + " " + eggs + " dans un saladier." +
                "\nFaire fondre au bain-marie " + chocolate + " " + butter +
                "\nRajouter le chocolat et le beurre dans le saladier." +
                "\nVerser le mélange dans un moule et enfourner pendant 25min à 180°C.";
    }

}
