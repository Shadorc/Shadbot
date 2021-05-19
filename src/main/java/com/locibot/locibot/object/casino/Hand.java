package com.locibot.locibot.object.casino;

import com.locibot.locibot.utils.FormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {

    private final List<Card> cards;

    public Hand(List<Card> cards) {
        this.cards = new ArrayList<>();
        this.deal(cards);
    }

    public Hand() {
        this(Collections.emptyList());
    }

    public void deal(Card card) {
        this.cards.add(card);
    }

    public void deal(List<Card> cards) {
        this.cards.addAll(cards);
    }

    public int count() {
        return this.getCards().size();
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(this.cards);
    }

    public int getValue() {
        int aceCount = 0;

        int value = 0;
        for (final Card card : this.cards) {
            if (card.value() == Value.ACE) {
                aceCount++;
            } else {
                // King, Queen and Jack have a number superior to 10 but their value is 10
                value += Math.min(10, card.value().getNumeric());
            }
        }

        if (aceCount > 0) {
            value += aceCount - 1;
            value += value + 11 > 21 ? 1 : 11;
        }

        return value;
    }

    public String format() {
        return "%s%nValue: %d"
                .formatted(
                        FormatUtil.format(this.getCards(),
                                card -> "`%s` %s".formatted(card.value().getIdent(), card.suit().getEmoji()),
                                " | "),
                        this.getValue());
    }

}
