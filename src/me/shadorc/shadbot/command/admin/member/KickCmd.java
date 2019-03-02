package me.shadorc.shadbot.command.admin.member;

import java.util.function.Consumer;

import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;

public class KickCmd extends RemoveMemberCmd {

	public KickCmd() {
		super("kick", Permission.KICK_MEMBERS, "kicked",
				(member, reason) -> member.kick(reason));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Kick user(s).")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
