package me.shadorc.shadbot.command.admin.member;

import java.util.function.Consumer;

import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;

public class BanCmd extends RemoveMemberCmd {

	public BanCmd() {
		super("ban", Permission.BAN_MEMBERS, "banned",
				(member, reason) -> member.ban(spec -> spec.setReason(reason).setDeleteMessageDays(7)));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Ban user(s) and delete his/their messages from the last 7 days.")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
