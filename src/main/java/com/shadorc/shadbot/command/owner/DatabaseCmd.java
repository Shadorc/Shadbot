package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;

public class DatabaseCmd extends BaseCmd {

    public DatabaseCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("database"), "db");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final StringBuilder strBuilder = new StringBuilder();
        try {
            final Process process = Runtime.getRuntime().exec(new String[]{"mongo", DatabaseManager.DATABASE_NAME, "--eval", arg});
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    strBuilder.append(line);
                }
            }
        } catch (final IOException err) {
            strBuilder.append(String.format(Emoji.RED_CROSS + " Error: %s", err.getMessage()));
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Evaluate a query with the MongoDB shell.")
                .addArg("query", false)
                .build();
    }
}
