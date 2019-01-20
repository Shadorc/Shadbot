package me.shadorc.shadbot.command.owner;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.shard.Shard;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "shardsinfo", "shards_info", "shards-info" })
public class ShardsInfoCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		return context.getAvatarUrl()
				.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
						.setAuthor("Shards states", null, avatarUrl)
						.addField("Index", FormatUtils.format(Shadbot.getShards().keySet().stream(), Object::toString, "\n"), true)
						.addField("State", FormatUtils.format(Shadbot.getShards().values().stream().map(Shard::getState), StringUtils::capitalizeEnum, "\n"), true)
						.addField("Fully ready", FormatUtils.format(Shadbot.getShards().values().stream().map(Shard::isFullyReady), Object::toString, "\n"), true))
				.flatMap(embed -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show shards info.")
				.build();
	}

}
