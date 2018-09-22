package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.Map;

import discord4j.core.object.entity.Role;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.message.ReactionMessage;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "iam" }, permissions = { Permission.ADD_REACTIONS })
public class IamCommand extends AbstractCommand {

	public static final Unicode REACTION = ReactionEmoji.unicode("✅");

	@Override
	public Mono<Void> execute(Context context) {
		return DiscordUtils.getRoles(context.getMessage())
				.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
				.collectList()
				.zipWith(context.getAvatarUrl())
				.flatMap(rolesAndAvatarUrl -> {
					final List<Role> roles = rolesAndAvatarUrl.getT1();
					final String avatarUrl = rolesAndAvatarUrl.getT2();

					if(roles.isEmpty()) {
						throw new MissingArgumentException();
					}

					final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Iam", null, avatarUrl)
							.setDescription(String.format("Click on %s to add you role(s): %s",
									REACTION.getRaw(),
									FormatUtils.format(roles, role -> String.format("`%s`", role.getMention()), "\n")));

					return new ReactionMessage(context.getClient(), context.getChannelId(), List.of(REACTION))
							.sendMessage(embed)
							.doOnSuccess(message -> {
								final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
								final Map<Snowflake, Snowflake> setting = dbGuild.getIamMessages();
								roles.stream()
										.map(Role::getId)
										.forEach(roleId -> setting.put(message.getId(), roleId));
								dbGuild.setSetting(SettingEnum.IAM_MESSAGES, setting);
							});
				})
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

}
