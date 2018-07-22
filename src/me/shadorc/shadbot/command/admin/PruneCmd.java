package me.shadorc.shadbot.command.admin;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "prune" })
public class PruneCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.getArg().orElse("");

		List<String> quotedElements = StringUtils.getQuotedElements(arg);

		if(arg.contains("\"") && quotedElements.isEmpty() || quotedElements.size() > 1) {
			throw new CommandException("You have forgotten a quote or have specified several quotes in quotation marks.");
		}

		final String words = quotedElements.isEmpty() ? null : quotedElements.get(0);

		return context.getMessage().getUserMentions()
				.collectList()
				.flatMap(mentions -> {

					// Remove everything from argument (users mentioned and quoted words) to keep only count if specified
					String argCleaned = StringUtils.remove(arg,
							FormatUtils.format(mentions, User::getMention, " "),
							String.format("\"%s\"", words))
							.trim();

					Integer count = NumberUtils.asPositiveInt(argCleaned);
					if(!argCleaned.isEmpty() && count == null) {
						throw new CommandException(String.format("`%s` is not a valid number. If you want to specify a word or a sentence, "
								+ "please include them in quotation marks. See `%shelp %s` for more information.",
								argCleaned, context.getPrefix(), this.getName()));
					}

					count = count == null ? 100 : Math.min(100, count);

					final List<Snowflake> mentionIds = mentions.stream().map(User::getId).collect(Collectors.toList());

					return context.getChannel()
							.flatMapMany(channel -> channel.getMessagesBefore(Snowflake.of(Instant.now())))
							.filter(message -> mentions.isEmpty()
									|| message.getAuthorId().map(mentionIds::contains).orElse(false))
							.filter(message -> words == null
									|| message.getContent().map(content -> content.contains(words)).orElse(false)
									|| this.getEmbedContent(message).contains(words))
							.take(count)
							.collectList();
				})
				.flatMap(messages -> BotUtils.bulkDelete(context.getChannel().cast(TextChannel.class), messages))
				.flatMap(deletedMessages -> BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (Requested by **%s**) %s deleted.",
						context.getUsername(), StringUtils.pluralOf(deletedMessages, "message")), context.getChannel()))
				.then();
	}

	private String getEmbedContent(Message message) {
		StringBuilder strBuilder = new StringBuilder();
		for(Embed embed : message.getEmbeds()) {
			for(Field field : embed.getFields()) {
				strBuilder.append(field.getName() + "\n" + field.getValue() + "\n");
			}
		}
		return strBuilder.toString();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Delete messages.")
				.addArg("@user(s)", "from these users", true)
				.addArg("\"words\"", "containing these words", true)
				.addArg("number", "number of messages to delete (max: 100)", true)
				.setExample(String.format("Delete **15** messages from user **@Shadbot** containing **hi guys**:"
						+ "%n`%s%s @Shadbot \"hi guys\" 15`", context.getPrefix(), this.getName()))
				.addField("Info", "Messages older than 2 weeks cannot be deleted.", false)
				.build();
	}

}
