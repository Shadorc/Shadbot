package com.shadorc.shadbot.core.command;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCmdGroup extends BaseCmd {

    private final Map<String, BaseCmd> cmds;

    public BaseCmdGroup(CommandCategory category, String name, String description, List<BaseCmd> cmds) {
        super(category, name, description);
        this.cmds = cmds.stream()
                .collect(Collectors.toUnmodifiableMap(BaseCmd::getName, Function.identity()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String cmdName = context.getEvent().getCommandInteraction().getOptions().get(0).getName();
        return this.cmds.get(cmdName).execute(context);
    }

    @Override
    public List<ApplicationCommandOptionData> buildOptions() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();
        for (final BaseCmd cmd : this.cmds.values()) {
            options.add(ApplicationCommandOptionData.builder()
                    .name(cmd.getName())
                    .description(cmd.getDescription())
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .options(cmd.buildOptions())
                    .build());
        }
        return options;
    }

}
