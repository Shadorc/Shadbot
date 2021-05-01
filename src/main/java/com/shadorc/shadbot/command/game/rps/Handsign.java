package com.shadorc.shadbot.command.game.rps;

import com.shadorc.shadbot.core.i18n.I18nContext;
import com.shadorc.shadbot.object.Emoji;

public enum Handsign {
    ROCK(Emoji.GEM),
    PAPER(Emoji.LEAF),
    SCISSORS(Emoji.SCISSORS);

    private final Emoji emoji;

    Handsign(Emoji emoji) {
        this.emoji = emoji;
    }

    public String getHandsign(I18nContext i18nContext) {
        return i18nContext.localize("handsign.%s".formatted(this.name().toLowerCase()));
    }

    public Emoji getEmoji() {
        return this.emoji;
    }

    public boolean isSuperior(Handsign other) {
        return this == Handsign.ROCK && other == Handsign.SCISSORS
                || this == Handsign.PAPER && other == Handsign.ROCK
                || this == Handsign.SCISSORS && other == Handsign.PAPER;
    }
}
