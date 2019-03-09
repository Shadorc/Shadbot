package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CleanDatabaseCmd extends BaseCmd {

	public CleanDatabaseCmd() {
		super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("clean_database", "clean-database", "cleandatabase"));
	}

	@Override
	public Mono<Void> execute(Context context) {
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(Emoji.INFO + " Cleaning database...", channel))
				.and(Flux.fromIterable(Shadbot.getDatabase().getDBGuilds())
				.flatMap(dbGuild -> context.getClient().getGuildById(dbGuild.getId())
						.doOnError(ClientException.class, err -> {
							if(err.getStatus().code() == 404 || err.getStatus().code() == 403) {
								LogUtils.info("Deleting guild ID: %d", dbGuild.getId().asLong());
								Shadbot.getDatabase().removeDBGuild(dbGuild.getId());
							}
						})
						.onErrorResume(err -> Mono.empty())
						.flatMapMany(guild -> Flux.fromIterable(dbGuild.getMembers())
								.flatMap(dbMember -> guild.getMemberById(dbMember.getId())
										.doOnNext(member -> {
											if(dbMember.getCoins() == 0) {
												dbGuild.removeMember(dbMember);
											}
										})
										.doOnError(ClientException.class, err -> {
											if(err.getStatus().code() == 404 || err.getStatus().code() == 403) {
												LogUtils.info("Deleting member ID: %d", dbMember.getId().asLong());
												dbGuild.removeMember(dbMember);
											}
										})
										.onErrorResume(err -> Mono.empty())))))
				.then(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " Database cleaned.", channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Clean the database by removing non-existent/forbidden guilds and members.")
				.build();
	}

}
