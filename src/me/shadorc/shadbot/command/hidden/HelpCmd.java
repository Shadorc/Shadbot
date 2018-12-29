package me.shadorc.shadbot.command.hidden;

import java.util.Collection;
import java.util.Map;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.HIDDEN, names = { "help" })
public class HelpCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		if(context.getArg().isPresent()) {
			final AbstractCommand cmd = CommandInitializer.getCommand(context.getArg().get());
			if(cmd == null) {
				return Mono.empty();
			}

			StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_HELPED, cmd);
			return cmd.getHelp(context)
					.flatMap(embed -> context.getChannel()
							.flatMap(channel -> BotUtils.sendMessage(embed, channel)))
					.then();
		}

		return context.getPermission()
				.flatMap(authorPerm -> Flux.fromIterable(CommandInitializer.getCommands().values())
						.distinct()
						.filter(cmd -> !cmd.getPermission().isSuperior(authorPerm))
						.filter(cmd -> context.isDm() || BotUtils.isCommandAllowed(context.getGuildId(), cmd))
						.collectMultimap(AbstractCommand::getCategory, cmd -> String.format("`%s%s`", context.getPrefix(), cmd.getName())))
				.zipWith(context.getAvatarUrl())
				.map(tuple -> {
					final Map<CommandCategory, Collection<String>> map = tuple.getT1();
					final String avatarUrl = tuple.getT2();

					final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Shadbot Help", null, avatarUrl)
							.setDescription(String.format("Any issues, questions or suggestions ?"
									+ " Join the [support server.](%s)"
									+ "%nGet more information by using `%s%s <command>`.",
									Config.SUPPORT_SERVER_URL, context.getPrefix(), this.getName()));

					for(final CommandCategory category : CommandCategory.values()) {
						if(!map.get(category).isEmpty() && !category.equals(CommandCategory.HIDDEN)) {
							embed.addField(String.format("%s Commands", category.toString()), String.join(" ", map.get(category)), false);
						}
					}

					return embed;
				})
				.flatMap(embed -> context.getChannel()
						.flatMap(channel -> BotUtils.sendMessage(embed, channel)))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show the list of available commands.")
				.build();
	}

}
