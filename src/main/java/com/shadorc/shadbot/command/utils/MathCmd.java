package com.shadorc.shadbot.command.utils;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;

public class MathCmd extends BaseCmd {

    private final DoubleEvaluator evaluator;
    private final DecimalFormat formatter;

    public MathCmd() {
        super(CommandCategory.UTILS, "math", "Calculate an expression");
        this.setDefaultRateLimiter();

        this.evaluator = new DoubleEvaluator();
        this.formatter = new DecimalFormat("#.##");
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("expression")
                        .description("Expression to evaluate (e.g. 2*cos(pi))")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String arg = context.getOption("expression").orElseThrow();
        return context.createFollowupMessage(Emoji.TRIANGULAR_RULER + " (**%s**) %s = %s",
                context.getAuthorName(), arg.replace("*", "\\*"),
                this.formatter.format(this.evaluator.evaluate(arg)))
                .onErrorMap(IllegalArgumentException.class, err -> new CommandException(err.getMessage()));
    }

}
