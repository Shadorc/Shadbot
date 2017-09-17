package me.shadorc.discordbot.command.admin.setting;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class NSFWSettingCmd implements SettingCmd {

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException {
		if(!BotUtils.hasPermission(context.getChannel(), Permissions.MANAGE_CHANNELS)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I can't execute this command due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Manage channels** is checked.",
					context.getChannel());
			LogUtils.info("{Guild ID: " + context.getChannel().getGuild().getLongID() + "} "
					+ "Shadbot wasn't allowed to manage channel.");
			return;
		}

		if(arg == null) {
			throw new MissingArgumentException();
		}

		boolean isNSFW;
		switch (arg) {
			case "toggle":
				isNSFW = !context.getChannel().isNSFW();
				break;
			case "enable":
				isNSFW = true;
				break;
			case "disable":
				isNSFW = false;
				break;
			default:
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid action. Use toggle, enable or disable.", context.getChannel());
				return;
		}

		context.getChannel().changeNSFW(isNSFW);
		BotUtils.sendMessage(Emoji.CHECK_MARK + " This channel is now " + (isNSFW ? "N" : "") + "SFW.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for setting: " + Setting.NSFW.toString())
				.appendDescription("**" + this.getDescription() + "**")
				.appendField("Argument", "**action** - toggle/enable/disable", false)
				.appendField("Usage", "`" + context.getPrefix() + "settings " + Setting.NSFW.toString() + " <action>`", false)
				.appendField("Example", "`" + context.getPrefix() + "settings " + Setting.NSFW.toString() + " toggle`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public String getDescription() {
		return "Change the current channel's NSFW state.";
	}

}
