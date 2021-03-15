package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.fun.FunCmd;
import com.shadorc.shadbot.command.gamestats.GameStatsCmd;
import com.shadorc.shadbot.command.image.ImageCmd;
import com.shadorc.shadbot.command.info.InfoCmd;
import com.shadorc.shadbot.command.moderation.ModerationCmd;
import com.shadorc.shadbot.command.owner.OwnerCmd;
import com.shadorc.shadbot.command.support.SupportCmd;
import com.shadorc.shadbot.command.todo.PingCmd;
import com.shadorc.shadbot.command.utils.UtilsCmd;
import discord4j.rest.service.ApplicationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class CommandManager {

    private static CommandManager instance;

    static {
        CommandManager.instance = new CommandManager();
    }

    private final Map<String, BaseCmd> commandsMap;

    private CommandManager() {
        this.commandsMap = CommandManager.initialize(
                new InfoCmd(), new SupportCmd(), new ImageCmd(), new ModerationCmd(), new OwnerCmd(), new UtilsCmd(),
                new FunCmd(), new GameStatsCmd(), new PingCmd());
    }

    private static Map<String, BaseCmd> initialize(BaseCmd... cmds) {
        final Map<String, BaseCmd> map = new LinkedHashMap<>(cmds.length);
        for (final BaseCmd cmd : cmds) {
            if (map.putIfAbsent(cmd.getName(), cmd) != null) {
                DEFAULT_LOGGER.error("Command name collision between {} and {}",
                        cmd.getClass().getSimpleName(), map.get(cmd.getName()).getClass().getSimpleName());
            }
        }
        DEFAULT_LOGGER.info("{} commands initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    public Mono<Long> register(ApplicationService applicationService, long applicationId) {
        return Flux.fromIterable(this.commandsMap.values())
                .flatMap(cmd -> cmd.register(applicationService, applicationId)
                        .onErrorResume(err -> Mono.fromRunnable(() ->
                                DEFAULT_LOGGER.error("An error occurred during '{}' registration: {}",
                                        cmd.getName(), err.getMessage()))))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} commands registered", cmdCount));
    }

    public Map<String, BaseCmd> getCommands() {
        return this.commandsMap;
    }

    public BaseCmd getCommand(String name) {
        return this.commandsMap.get(name);
    }

    public static CommandManager getInstance() {
        return CommandManager.instance;
    }
}
