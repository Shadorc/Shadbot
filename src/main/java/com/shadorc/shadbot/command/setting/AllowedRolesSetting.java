package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.database.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AllowedRolesSetting extends SubCmd {

    private enum Action {
        ADD, REMOVE
    }

    public AllowedRolesSetting(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.SETTING, CommandPermission.ADMIN,
                "allowed_roles", "Manage role(s) that can interact with Shadbot");

        this.addOption(option -> option.name("action")
                .description("Whether to add or remove a role from the allowed ones")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("role1")
                .description("The first role")
                .required(true)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
        this.addOption(option -> option.name("role2")
                .description("The second role")
                .required(false)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
        this.addOption(option -> option.name("role3")
                .description("The third role")
                .required(false)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
        this.addOption(option -> option.name("role4")
                .description("The fourth role")
                .required(false)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
        this.addOption(option -> option.name("role5")
                .description("The fifth role")
                .required(false)
                .type(ApplicationCommandOptionType.ROLE.getValue()));

/* TODO               .addField("Info", "**server owner** and **administrators** "
                        + "will always be able to interact with Shadbot.", false)
 */
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Flux<Role> getRoles = Flux.fromStream(IntStream.range(1, 6).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));

        return getRoles
                .collectList()
                .flatMap(mentionedRoles -> {
                    final DBGuild dbGuild = context.getDbGuild();
                    final Set<Snowflake> allowedRoleIds = dbGuild.getSettings().getAllowedRoleIds();
                    final Set<Snowflake> mentionedRoleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toUnmodifiableSet());

                    final StringBuilder strBuilder = new StringBuilder();
                    if (action == Action.ADD) {
                        if (!allowedRoleIds.addAll(mentionedRoleIds)) {
                            return context.createFollowupMessage(Emoji.GREY_EXCLAMATION,
                                    context.localize("allowedroles.already.added"));
                        }
                        strBuilder.append(Emoji.CHECK_MARK + context.localize("allowedroles.added")
                                .formatted(FormatUtil.format(mentionedRoles, role -> "`@%s`".formatted(role.getName()), ", ")));
                    } else {
                        if (!allowedRoleIds.removeAll(mentionedRoleIds)) {
                            return context.createFollowupMessage(Emoji.GREY_EXCLAMATION,
                                    context.localize("allowedroles.already.removed"));
                        }
                        strBuilder.append(Emoji.CHECK_MARK + context.localize("allowedroles.removed")
                                .formatted(FormatUtil.format(mentionedRoles, role -> "`@%s`".formatted(role.getName()), ", ")));

                        if (allowedRoleIds.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + context.localize("allowedroles.reset"));
                        }
                    }

                    return dbGuild.updateSetting(Setting.ALLOWED_ROLES, allowedRoleIds)
                            .then(context.createFollowupMessage(strBuilder.toString()));
                });
    }

}
