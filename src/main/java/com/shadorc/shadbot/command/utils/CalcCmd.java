package com.shadorc.shadbot.command.utils;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;

public class CalcCmd extends BaseCmd {

    private final DoubleEvaluator evaluator;
    private final DecimalFormat formatter;

    public CalcCmd() {
        super(CommandCategory.UTILS, List.of("calc", "math"));
        this.setDefaultRateLimiter();

        this.evaluator = new DoubleEvaluator();
        this.formatter = new DecimalFormat("#.##");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.TRIANGULAR_RULER + " (**%s**) %s = %s",
                        context.getUsername(), arg.replace("*", "\\*"),
                        this.formatter.format(this.evaluator.evaluate(arg))), channel))
                .onErrorMap(IllegalArgumentException.class, err -> new CommandException(err.getMessage()))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Calculate an expression.")
                .addArg("expression", false)
                .setExample(String.format("`%s%s 3+3*3+3`%n`%s%s 2*cos(pi)`",
                        context.getPrefix(), this.getName(), context.getPrefix(), this.getName()))
                .build();
    }

}
