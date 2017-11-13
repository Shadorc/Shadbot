package me.shadorc.discordbot.command.game.blackjack;

import java.util.ArrayList;
import java.util.List;

import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.game.Card;
import me.shadorc.discordbot.utils.game.Card.Sign;

public class BlackjackUtils {

	public static List<Card> pickCards(int count) {
		List<Card> cards = new ArrayList<>();
		for(int i = 0; i < count; i++) {
			cards.add(new Card(MathUtils.rand(1, 13), Sign.values()[MathUtils.rand(Sign.values().length)]));
		}
		return cards;
	}

	public static String formatCards(List<Card> cards) {
		return FormatUtils.formatList(cards, card -> "`" + card.getName() + "` " + card.getSign().getEmoji(), " | ")
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
			for(int i = 0; i < aceCount - 1; i++) {
				value += 1;
			}
			value += (value + 11 > 21 ? 1 : 11);
		}

		return value;
	}
}
