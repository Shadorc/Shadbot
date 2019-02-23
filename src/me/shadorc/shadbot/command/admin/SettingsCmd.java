package me.shadorc.shadbot.command.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "setting", "settings" })
public class SettingsCmd extends AbstractCommand {

	private static final Map<SettingEnum, AbstractSetting> SETTINGS_MAP = new HashMap<>();

	static {
		final Reflections reflections = new Reflections(SettingsCmd.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(final Class<?> settingClass : reflections.getTypesAnnotatedWith(Setting.class)) {
			final String settingName = settingClass.getSimpleName();
			if(!AbstractSetting.class.isAssignableFrom(settingClass)) {
				LogUtils.error(String.format("An error occurred while generating setting, %s cannot be cast to %s.",
						settingName, AbstractSetting.class.getSimpleName()));
				continue;
			}

			try {
				final AbstractSetting settingCmd = (AbstractSetting) settingClass.getConstructor().newInstance();
				if(SETTINGS_MAP.putIfAbsent(settingCmd.getSetting(), settingCmd) != null) {
					LogUtils.error(String.format("Command name collision between %s and %s",
							settingName, SETTINGS_MAP.get(settingCmd.getSetting()).getClass().getSimpleName()));
					continue;
				}
			} catch (final Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing setting %s.", settingName));
			}
		}
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(1, Integer.MAX_VALUE);

		if("show".equals(args.get(0))) {
			return this.show(context)
					.flatMap(embed -> context.getChannel()
							.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
					.then();
		}

		final SettingEnum settingEnum = Utils.getEnum(SettingEnum.class, args.get(0));
		final AbstractSetting setting = SETTINGS_MAP.get(settingEnum);
		if(setting == null) {
			throw new CommandException(String.format("Setting `%s` does not exist. Use `%shelp %s` to see all available settings.",
					args.get(0), context.getPrefix(), this.getName()));
		}

		final String arg = args.size() == 2 ? args.get(1) : null;
		if("help".equals(arg)) {
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(this.getHelp(context, setting), channel))
					.then();
		}

		try {
			return setting.execute(context);
		} catch (MissingArgumentException err) {
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(
							Emoji.WHITE_FLAG + " Some arguments are missing, here is the help for this setting.", this.getHelp(context, setting), channel))
					.then();
		}
	}

	private Mono<Consumer<EmbedCreateSpec>> show(Context context) {
		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(context.getGuildId());
		final StringBuilder settingsStr = new StringBuilder();

		if(!dbGuild.getPrefix().equals(Config.DEFAULT_PREFIX)) {
			settingsStr.append(String.format("**Prefix:** %s", context.getPrefix()));
		}

		if(dbGuild.getDefaultVol().intValue() != Config.DEFAULT_VOLUME) {
			settingsStr.append(String.format("%n**Default volume:** %d%%", dbGuild.getDefaultVol()));
		}

		if(!dbGuild.getBlacklistedCmd().isEmpty()) {
			settingsStr.append(String.format("%n**Blacklisted commands:**%n\t%s",
					String.join("\n\t", dbGuild.getBlacklistedCmd())));
		}

		dbGuild.getJoinMessage()
				.ifPresent(joinMessage -> settingsStr.append(String.format("%n**Join message:**%n%s", joinMessage)));
		dbGuild.getLeaveMessage()
				.ifPresent(leaveMessage -> settingsStr.append(String.format("%n**Leave message:**%n%s", leaveMessage)));

		final Mono<Void> autoMessageChannelStr = Mono.justOrEmpty(dbGuild.getMessageChannelId())
				.map(Snowflake::of)
				.flatMap(context.getClient()::getChannelById)
				.map(Channel::getMention)
				.map(channel -> settingsStr.append(String.format("%n**Auto message channel:** %s", channel)))
				.then();

		final Mono<Void> allowedChannelsStr = Flux.fromIterable(dbGuild.getAllowedTextChannels())
				.map(Snowflake::of)
				.flatMap(context.getClient()::getChannelById)
				.map(Channel::getMention)
				.collectList()
				.filter(channels -> !channels.isEmpty())
				.map(channels -> settingsStr.append(String.format("%n**Allowed channels:**%n\t%s", String.join("\n\t", channels))))
				.then();

		final Mono<Void> autoRolesStr = Flux.fromIterable(dbGuild.getAutoRoles())
				.map(Snowflake::of)
				.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
				.map(Role::getMention)
				.collectList()
				.filter(roles -> !roles.isEmpty())
				.map(roles -> settingsStr.append(String.format("%n**Auto-roles:**%n\t%s", String.join("\n\t", roles))))
				.then();

		final Mono<Void> allowedRolesStr = Flux.fromIterable(dbGuild.getAllowedRoles())
				.map(Snowflake::of)
				.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
				.map(Role::getMention)
				.collectList()
				.filter(roles -> !roles.isEmpty())
				.map(roles -> settingsStr.append(String.format("%n**Allowed roles:**%n\t%s", String.join("\n\t", roles))))
				.then();

		return autoMessageChannelStr
				.then(allowedChannelsStr)
				.then(autoRolesStr)
				.then(allowedRolesStr)
				.thenReturn(EmbedUtils.getDefaultEmbed()
						.andThen(embed -> embed.setAuthor("Settings", null, context.getAvatarUrl())
								.setDescription(
										settingsStr.length() == 0 ? "There is no custom settings for this server." : settingsStr.toString())));
	}

	private Consumer<EmbedCreateSpec> getHelp(Context context, AbstractSetting setting) {
		return setting.getHelp(context)
				.andThen(embed -> embed.setAuthor(String.format("Help for setting: %s", setting.getName()), null, context.getAvatarUrl())
						.setDescription(String.format("**%s**", setting.getDescription())));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		final HelpBuilder embed = new HelpBuilder(this, context)
				.setThumbnail("http://www.emoji.co.uk/files/emoji-one/objects-emoji-one/1898-gear.png")
				.setDescription("Change Shadbot's settings for this server.")
				.addArg("name", false)
				.addArg("args", false)
				.addField("Additional Help", String.format("`%s%s <name> help`",
						context.getPrefix(), this.getName()), false)
				.addField("Current settings", String.format("`%s%s show`",
						context.getPrefix(), this.getName()), false);

		SETTINGS_MAP.values().stream()
				.forEach(setting -> embed.addField(String.format("Name: %s", setting.getName()),
						setting.getDescription(),
						false));

		return embed.build();
	}

}
