package com.shadorc.shadbot.command.owner;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlQueryLogicError;
import com.rethinkdb.net.Connection;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class DatabaseCmd extends BaseCmd {

    private final Connection connection;

    public DatabaseCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("database"));

        this.connection = RethinkDB.r.connection()
                .hostname(Credentials.get(Credential.DATABASE_HOST))
                .port(Integer.parseInt(Credentials.get(Credential.DATABASE_PORT)))
                .user(Credentials.get(Credential.DATABASE_USER), Credentials.get(Credential.DATABASE_PASSWORD))
                .connect();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        // TODO
        return context.getChannel()
                .flatMap(channel -> {
                    try {
                        final String response = RethinkDB.r.js(arg).run(this.connection).toString();
                        return DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Response: %s", response), channel);
                    } catch (final ReqlQueryLogicError err) {
                        return DiscordUtils.sendMessage(String.format(Emoji.RED_CROSS + " Error: %s", err.getMessage()), channel);
                    }
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Execute a JavaScript expression on the database.")
                .addArg("js", false)
                .build();
    }

}
