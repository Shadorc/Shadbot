package com.shadorc.shadbot.command.info.group;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InfoCmd extends BaseCmd {

    private final Map<String, BaseCmd> infoCmds;

    public InfoCmd() {
        super(CommandCategory.INFO, "info", "Show specific information");
        this.infoCmds = List.of(new BotInfoCmd(), new ServerInfoCmd(), new UserInfoCmd()).stream()
                .collect(Collectors.toUnmodifiableMap(BaseCmd::getName, Function.identity()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String cmdName = context.getEvent().getCommandInteractionData().options()
                .toOptional().orElseThrow().get(0).name();
        return this.infoCmds.get(cmdName).execute(context);
    }

    @Override
    public List<ApplicationCommandOptionData> buildOptions() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();
        for (final BaseCmd cmd : this.infoCmds.values()) {
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
