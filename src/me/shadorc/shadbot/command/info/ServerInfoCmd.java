package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import discord4j.core.object.Region;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Image.Format;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "serverinfo", "server_info", "server-info" })
public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);

	@Override
	public Mono<Void> execute(Context context) {
		return Mono.zip(context.getGuild(),
				context.getGuild().flatMap(Guild::getOwner),
				context.getGuild().flatMapMany(Guild::getChannels).collectList(),
				context.getGuild().flatMap(Guild::getRegion),
				context.getAvatarUrl())
				.map(tuple5 -> {
					final Guild guild = tuple5.getT1();
					final Member owner = tuple5.getT2();
					final List<GuildChannel> channels = tuple5.getT3();
					final Region region = tuple5.getT4();
					final String avatarUrl = tuple5.getT5();

					final String creationDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(guild.getId().getTimestamp()).format(this.dateFormatter),
							FormatUtils.longDuration(guild.getId().getTimestamp()));
					final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
					final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

					return EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Server Info: %s", guild.getName()), null, avatarUrl)
							.setThumbnail(guild.getIconUrl(Format.JPEG).get())
							.addField("Owner", owner.getUsername(), true)
							.addField("Server ID", guild.getId().asString(), true)
							.addField("Creation date", creationDate, true)
							.addField("Region", region.getName(), true)
							.addField("Channels", String.format("**Voice:** %d", voiceChannels)
									+ String.format("%n**Text:** %d", textChannels), true)
							.addField("Members", Integer.toString(guild.getMemberCount().getAsInt()), true);
				})
				.flatMap(embed -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show info about this server.")
				.build();
	}

}
