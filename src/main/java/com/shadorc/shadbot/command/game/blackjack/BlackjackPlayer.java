package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.object.casino.Card;
import com.shadorc.shadbot.object.casino.Hand;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;

public class BlackjackPlayer extends GamblerPlayer {

    private final Hand hand;

    private boolean isDoubleDown;
    private boolean isStanding;

    public BlackjackPlayer(Snowflake guildId, Snowflake userId, String username, long bet) {
        super(guildId, userId, username, bet);
        this.hand = new Hand();
        this.isDoubleDown = false;
        this.isStanding = false;
    }

    public void hit(Card card) {
        this.hand.deal(card);

        if (this.hand.getValue() >= 21) {
            this.stand();
        }
    }

    public void stand() {
        this.isStanding = true;
    }

    public void doubleDown(Card card) {
        this.isDoubleDown = true;
        this.hit(card);
        this.stand();
    }

    public ImmutableEmbedFieldData format() {
        final StringBuilder name = new StringBuilder("%s's hand".formatted(this.getUsername()));
        if (this.isStanding) {
            name.append(' ')
                    .append("(Stand)");
        }
        if (this.isDoubleDown) {
            name.append(' ')
                    .append("(Double down)");
        }

        return ImmutableEmbedFieldData.of(name.toString(), this.hand.format(), Possible.of(true));
    }

    @Override
    public long getBet() {
        return super.getBet() * (this.isDoubleDown ? 2 : 1);
    }

    public Hand getHand() {
        return this.hand;
    }

    public boolean isStanding() {
        return this.isStanding;
    }

}
