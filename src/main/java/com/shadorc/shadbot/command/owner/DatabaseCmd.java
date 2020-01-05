package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class DatabaseCmd extends BaseCmd {

    private static final int MAX_WIDTH = Message.MAX_CONTENT_LENGTH * 3 / 4;

    public DatabaseCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, List.of("database"), "db");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final StringBuilder strBuilder = new StringBuilder();
        try {
            final Process process = Runtime.getRuntime().exec(new String[]{"mongo", Config.DATABASE_NAME, "--eval", arg});
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    strBuilder.append(String.format("%s\n", line));
                }
            }
        } catch (final IOException err) {
            strBuilder.append(String.format(Emoji.RED_CROSS + " Error: %s", err.getMessage()));
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(StringUtils.abbreviate(strBuilder.toString(), MAX_WIDTH), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Evaluate a query with the MongoDB shell.")
                .addArg("query", false)
                .build();
    }
}
