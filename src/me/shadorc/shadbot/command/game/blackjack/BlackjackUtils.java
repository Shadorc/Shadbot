package me.shadorc.shadbot.command.game.blackjack;

import java.util.List;

import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.command.Card;

public class BlackjackUtils {

	public static String formatCards(List<Card> cards) {
		return FormatUtils.format(cards, card -> String.format("`%s` %s", card.getName(), card.getSign().getEmoji()), " | ")
				+ "\nValue: " + BlackjackUtils.getValue(cards);
	}

	public static int getValue(List<Card> cards) {
		int aceCount = 0;

		int value = 0;
		for(Card card : cards) {
			if(card.getNum() == 1) {
				aceCount++;
				continue;
			}
			value += Math.min(10, card.getNum());
		}

		if(aceCount > 0) {
			value += aceCount - 1;
			value += value + 11 > 21 ? 1 : 11;
		}

		return value;
	}
}
