package com.shadorc.shadbot.core.command;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.*;

public abstract class GroupCmd extends Cmd {

    private final Map<String, SubCmd> cmds;

    public GroupCmd(CommandCategory category, CommandPermission permission, String description) {
        super(category, permission, category.name().toLowerCase(), description);
        this.cmds = new LinkedHashMap<>();
    }

    public GroupCmd(CommandCategory category, String description) {
        this(category, CommandPermission.USER, description);
    }

    public Collection<SubCmd> getSubCommands() {
        return this.cmds.values();
    }

    public void addSubCommand(SubCmd subCmd) {
        this.cmds.put(subCmd.getName(), subCmd);
    }

    @Override
    public Mono<?> execute(Context context) {
        throw new IllegalStateException("Group command should not be executed");
    }

    @Override
    public List<ApplicationCommandOptionData> getOptions() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();
        for (final Cmd cmd : this.getSubCommands()) {
            options.add(ApplicationCommandOptionData.builder()
                    .name(cmd.getName())
                    .description(cmd.getDescription())
                    .type(cmd.getType().orElse(ApplicationCommandOptionType.SUB_COMMAND).getValue())
                    .options(cmd.getOptions())
                    .build());
        }
        return Collections.unmodifiableList(options);
    }

}
