package me.shadorc.shadbot.command.admin.member;

import java.util.function.Consumer;

import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class BanCmd extends RemoveMemberCmd {

	public BanCmd() {
		super("ban", "banned", Permission.BAN_MEMBERS);
	}

	@Override
	public Mono<Void> action(Member member, String reason) {
		return member.ban(spec -> spec.setReason(reason).setDeleteMessageDays(7));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Ban user and delete his messages from the last 7 days.")
				.addArg("@user", false)
				.addArg("reason", true)
				.build();
	}

}
