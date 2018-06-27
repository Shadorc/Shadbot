package me.shadorc.shadbot.command.currency;

import java.util.ArrayList;
import java.util.List;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBMember;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "transfer" })
public class TransferCoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		if(context.getMessage().getUserMentionIds().isEmpty()) {
			throw new MissingArgumentException();
		}

		Snowflake senderUserId = context.getAuthorId();
		Snowflake receiverUserId = new ArrayList<>(context.getMessage().getUserMentionIds()).get(0);
		if(receiverUserId.equals(senderUserId)) {
			throw new CommandException("You cannot transfer coins to yourself.");
		}

		Integer coins = NumberUtils.asPositiveInt(args.get(0));
		if(coins == null) {
			throw new CommandException(
					String.format("`%s` is not a valid amount for coins.", args.get(0)));
		}

		context.getGuild().subscribe(guild -> {
			DBMember dbSender = Database.getDBMember(guild.getId(), senderUserId);
			if(dbSender.getCoins() < coins) {
				context.getAuthor().subscribe(author -> BotUtils.sendMessage(TextUtils.notEnoughCoins(author), context.getChannel()));
				return;
			}

			DBMember dbReceiver = Database.getDBMember(guild.getId(), receiverUserId);
			if(dbReceiver.getCoins() + coins >= Config.MAX_COINS) {
				context.getClient().getUserById(receiverUserId)
						.subscribe(user -> {
							String message = String.format(
									Emoji.BANK + " This transfer cannot be done because %s would exceed the maximum coins cap.",
									user.getUsername());
							BotUtils.sendMessage(message, context.getChannel());
						});
				return;
			}

			dbSender.addCoins(-coins);
			dbReceiver.addCoins(coins);

			context.getAuthor().zipWith(context.getClient().getUserById(senderUserId)).subscribe(senderAndReceiver -> {
				String message = String.format(Emoji.BANK + " %s has transfered **%s** to %s",
						senderAndReceiver.getT1().getMention(), FormatUtils.formatCoins(coins), senderAndReceiver.getT2().getMention());
				BotUtils.sendMessage(message, context.getChannel());
			});
		});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Transfer coins to the mentioned user.")
				.addArg("coins", false)
				.addArg("@user", false)
				.build();
	}
}
