package me.shadorc.shadbot.command.owner;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.help.HelpBuilder;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class DatabaseCmd extends BaseCmd {

    public DatabaseCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("database"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, 2);

        final Long guildId = NumberUtils.asPositiveLong(args.get(0));
        if (guildId == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid guild ID.",
                    args.get(0))));
        }

        return context.getClient().getGuildById(Snowflake.of(guildId))
                .switchIfEmpty(Mono.error(new CommandException("Guild not found.")))
                .flatMap(guild -> {
                    if (args.size() == 1) {
                        return Mono.just(DatabaseManager.getInstance().getDBGuild(guild.getId()).toString());
                    }

                    final Long memberId = NumberUtils.asPositiveLong(args.get(1));
                    if (memberId == null) {
                        return Mono.error(new CommandException(String.format("`%s` is not a valid member ID.",
                                args.get(1))));
                    }

                    return context.getClient().getMemberById(Snowflake.of(guildId), Snowflake.of(memberId))
                            .switchIfEmpty(Mono.error(new CommandException("Member not found.")))
                            .map(member -> DatabaseManager.getInstance().getDBMember(guild.getId(), member.getId()).toString());
                })
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Return data about a member / guild.")
                .addArg("guildID", false)
                .addArg("memberID", true)
                .build();
    }

}
