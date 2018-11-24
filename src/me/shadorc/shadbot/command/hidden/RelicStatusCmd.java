package me.shadorc.shadbot.command.hidden;

import java.util.List;
import java.util.concurrent.TimeUnit;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.premium.Relic;
import me.shadorc.shadbot.data.premium.Relic.RelicType;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.HIDDEN, names = { "contributor_status", "donator_status", "relic_status" })
public class RelicStatusCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final List<Relic> relics = Shadbot.getPremium().getRelicsForUser(context.getAuthorId());
		if(relics.isEmpty()) {
			return BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You are not a donator. If you like Shadbot, "
					+ "you can help me keep it alive by making a donation on **%s**."
					+ "%nAll donations are important and really help me %s",
					context.getUsername(), Config.PATREON_URL, Emoji.HEARTS), context.getChannel())
					.then();
		}

		return Flux.fromIterable(relics)
				.map(relic -> {
					final StringBuilder contentBld = new StringBuilder(String.format("**ID:** %s", relic.getId()));

					relic.getGuildId().ifPresent(guildId -> contentBld.append(String.format("%n**Guild ID:** %d", guildId.asLong())));

					contentBld.append(String.format("%n**Duration:** %d days", TimeUnit.MILLISECONDS.toDays(relic.getDuration())));
					if(!relic.isExpired() && relic.getActivationTime().isPresent()) {
						final long millisLeft = relic.getDuration() - TimeUtils.getMillisUntil(relic.getActivationTime().getAsLong());
						contentBld.append(String.format("%n**Expires in:** %d days", TimeUnit.MILLISECONDS.toDays(millisLeft)));
					}

					final StringBuilder titleBld = new StringBuilder();
					if(relic.getType().equals(RelicType.GUILD.toString())) {
						titleBld.append("Legendary ");
					}
					titleBld.append(String.format("Relic (%s)", relic.isExpired() ? "Expired" : "Activated"));

					return new EmbedFieldEntity(titleBld.toString(), contentBld.toString(), false);
				})
				.collectList()
				.zipWith(context.getAvatarUrl())
				.map(fieldsAndAvatarUrl -> {
					final List<EmbedFieldEntity> fields = fieldsAndAvatarUrl.getT1();
					final String avatarUrl = fieldsAndAvatarUrl.getT2();

					final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Contributor Status", null, avatarUrl)
							.setThumbnail("https://orig00.deviantart.net/24e1/f/2015/241/8/7/relic_fragment_by_yukimemories-d97l8c8.png");

					fields.stream()
							.forEach(field -> embed.addField(field.getName(), field.getValue(), field.isInline()));

					return embed;
				})
				.flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show your contributor status.")
				.build();
	}
}
