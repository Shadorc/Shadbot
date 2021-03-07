package com.shadorc.shadbot.command.utils;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;

public class MathCmd extends BaseCmd {

    private final DoubleEvaluator evaluator;
    private final DecimalFormat formatter;

    public MathCmd() {
        super(CommandCategory.UTILS, "math", "Calculate an expression");
        this.addOption("expression",
                "Expression to evaluate (e.g. 2*cos(pi))",
                true,
                ApplicationCommandOptionType.STRING);

        this.evaluator = new DoubleEvaluator();
        this.formatter = new DecimalFormat("#.##");
    }

    @Override
    public Mono<?> execute(Context context) {
        final String arg = context.getOptionAsString("expression").orElseThrow();
        return context.createFollowupMessage(Emoji.TRIANGULAR_RULER + " (**%s**) %s = %s",
                context.getAuthorName(), arg.replace("*", "\\*"),
                this.formatter.format(this.evaluator.evaluate(arg)))
                .onErrorMap(IllegalArgumentException.class, err -> new CommandException(err.getMessage()));
    }

}
