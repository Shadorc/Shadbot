package com.locibot.locibot.object.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class Deck {

    private final Stack<Card> cards;

    public Deck() {
        this.cards = new Stack<>();
        for (final Suit suit : Suit.values()) {
            for (final Value value : Value.values()) {
                this.cards.add(new Card(suit, value));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(this.cards);
    }

    public Card pick() {
        return this.cards.pop();
    }

    public List<Card> pick(int count) {
        final List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(this.pick());
        }
        return cards;
    }

}
