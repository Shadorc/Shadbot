package com.shadorc.shadbot.object;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.util.annotation.Nullable;

import java.awt.*;
import java.util.function.Consumer;

public class LogBuilder {

    public enum LogType {
        INFO, WARN, ERROR;
    }

    private final LogType type;
    @Nullable
    private String message;
    @Nullable
    private Throwable error;
    @Nullable
    private String input;

    public LogBuilder(LogType type) {
        this.type = type;
    }

    public LogBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public LogBuilder setError(Throwable error) {
        this.error = error;
        return this;
    }

    public LogBuilder setInput(String input) {
        this.input = input;
        return this;
    }

    public Consumer<EmbedCreateSpec> build() {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor(String.format("%s (Version: %s)", StringUtils.capitalizeEnum(this.type), Config.VERSION), null, null);

                    if (this.message != null && !this.message.isBlank()) {
                        embed.setDescription(org.apache.commons.lang3.StringUtils.abbreviate(this.message, Embed.MAX_DESCRIPTION_LENGTH));
                    }

                    switch (this.type) {
                        case ERROR:
                            embed.setColor(Color.RED);
                            break;
                        case WARN:
                            embed.setColor(Color.ORANGE);
                            break;
                        case INFO:
                            embed.setColor(Color.GREEN);
                            break;
                        default:
                            embed.setColor(Color.BLUE);
                            break;
                    }

                    if (this.error != null) {
                        embed.addField("Error type", this.error.getClass().getSimpleName(), false);
                        if (this.error.getMessage() != null && !this.error.getMessage().isBlank()) {
                            embed.addField("Error message", org.apache.commons.lang3.StringUtils.abbreviate(this.error.getMessage(), Embed.Field.MAX_VALUE_LENGTH), false);
                        }
                    }

                    if (this.input != null && !this.input.isBlank()) {
                        embed.addField("Input", org.apache.commons.lang3.StringUtils.abbreviate(this.input, Embed.Field.MAX_VALUE_LENGTH), false);
                    }
                });
    }

}
