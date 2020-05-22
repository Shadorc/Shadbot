package com.shadorc.shadbot.command.admin;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.bean.setting.IamBean;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.setting.Iam;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.ReactionMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IamCmd extends BaseCmd {

    public static final Unicode REACTION = ReactionEmoji.unicode("✅");

    public IamCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("iam"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final List<String> quotedElements = StringUtils.getQuotedElements(arg);
        if (quotedElements.isEmpty() && arg.contains("\"")) {
            return Mono.error(new CommandException("One quotation mark is missing."));
        }
        if (quotedElements.size() > 1) {
            return Mono.error(new CommandException("You should specify only one text in quotation marks."));
        }

        final Flux<Role> getRoles = context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, StringUtils.remove(arg, quotedElements)));

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.MANAGE_ROLES, Permission.ADD_REACTIONS)
                        .then(getRoles.collectList())
                        .flatMap(roles -> {
                            if (roles.isEmpty()) {
                                return Mono.error(new MissingArgumentException());
                            }

                            final StringBuilder description = new StringBuilder();
                            if (quotedElements.isEmpty()) {
                                description.append(String.format("Click on %s to get role(s): %s", REACTION.getRaw(),
                                        FormatUtils.format(roles, role -> String.format("`@%s`", role.getName()), "\n")));
                            } else {
                                description.append(quotedElements.get(0));
                            }

                            final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                                    .andThen(embed -> embed.setAuthor(String.format("Iam: %s",
                                            FormatUtils.format(roles, role -> String.format("@%s", role.getName()), ", ")),
                                            null, context.getAvatarUrl())
                                            .setDescription(description.toString()));

                            return new ReactionMessage(context.getClient(), context.getChannelId(), List.of(REACTION))
                                    .send(embedConsumer)
                                    .zipWith(DatabaseManager.getGuilds().getDBGuild(context.getGuildId()))
                                    .flatMap(tuple -> {
                                        final Message message = tuple.getT1();
                                        final DBGuild dbGuild = tuple.getT2();

                                        // Converts the new message to an IamBean
                                        final List<IamBean> iamList = roles.stream()
                                                .map(Role::getId)
                                                .map(roleId -> new Iam(message.getId(), roleId))
                                                .map(Iam::getBean)
                                                .collect(Collectors.toList());

                                        // Add previous Iam to the new one
                                        iamList.addAll(dbGuild.getSettings()
                                                .getIam()
                                                .stream()
                                                .map(Iam::getBean)
                                                .collect(Collectors.toList()));

                                        return dbGuild.setSetting(Setting.IAM_MESSAGES, iamList);
                                    });
                        }))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription(String.format("Send a message with a reaction, users will be able to get the role(s) "
                        + "associated with the message by clicking on %s", REACTION.getRaw()))
                .addArg("@role(s)", false)
                .addArg("\"text\"", "Replace the default text", true)
                .build();
    }

}
