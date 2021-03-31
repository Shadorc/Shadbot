package com.shadorc.shadbot.core.command;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCmdGroup extends BaseCmd {

    private final Map<String, BaseCmd> cmds;

    public BaseCmdGroup(CommandCategory category, CommandPermission permission, String name, String description, List<BaseCmd> cmds) {
        super(category, permission, name, description);
        this.cmds = BaseCmdGroup.buildCmdsMap(cmds);
    }

    public BaseCmdGroup(CommandCategory category, String name, String description, List<BaseCmd> cmds) {
        super(category, name, description);
        this.cmds = BaseCmdGroup.buildCmdsMap(cmds);
    }

    private static Map<String, BaseCmd> buildCmdsMap(List<BaseCmd> cmds) {
        return cmds.stream().collect(Collectors.toUnmodifiableMap(BaseCmd::getName, Function.identity()));
    }

    public Collection<BaseCmd> getSubCommands() {
        return Collections.unmodifiableCollection(this.cmds.values());
    }

    @Override
    public Mono<?> execute(Context context) {
        throw new IllegalStateException();
    }

    @Override
    public List<ApplicationCommandOptionData> getOptions() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();
        for (final BaseCmd cmd : this.cmds.values()) {
            options.add(ApplicationCommandOptionData.builder()
                    .name(cmd.getName())
                    .description(cmd.getDescription())
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .options(cmd.getOptions())
                    .build());
        }
        return Collections.unmodifiableList(options);
    }

}
