package me.shadorc.shadbot.command.admin.member;

import java.util.function.Consumer;

import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;

public class SoftBanCmd extends RemoveMemberCmd {

	public SoftBanCmd() {
		super("softban", Permission.BAN_MEMBERS, "softbanned",
				(member, reason) -> member.ban(spec -> spec.setReason(reason).setDeleteMessageDays(7))
						.then(member.unban()));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Ban and instantly unban user(s).\nIt's like kicking him/them but it also deletes his/their messages "
						+ "from the last 7 days.")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
