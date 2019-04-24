package me.shadorc.shadbot.object.casino;

public class Card {

    private final Value value;
    private final Suit suit;

    public Card(Suit suit, Value value) {
        this.suit = suit;
        this.value = value;
    }

    public Value getValue() {
        return this.value;
    }

    public Suit getSuit() {
        return this.suit;
    }

}
