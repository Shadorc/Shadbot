package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.currency.CurrencyGroup;
import com.shadorc.shadbot.command.donator.DonatorGroup;
import com.shadorc.shadbot.command.fun.FunGroup;
import com.shadorc.shadbot.command.game.GameGroup;
import com.shadorc.shadbot.command.gamestats.GameStatsGroup;
import com.shadorc.shadbot.command.image.ImageGroup;
import com.shadorc.shadbot.command.info.InfoGroup;
import com.shadorc.shadbot.command.moderation.ModerationGroup;
import com.shadorc.shadbot.command.music.MusicGroup;
import com.shadorc.shadbot.command.owner.OwnerGroup;
import com.shadorc.shadbot.command.setting.SettingGroup;
import com.shadorc.shadbot.command.support.SupportGroup;
import com.shadorc.shadbot.command.util.UtilGroup;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.ExceptionHandler;
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
                new InfoGroup(), new SupportGroup(), new ImageGroup(), new ModerationGroup(), new OwnerGroup(),
                new UtilGroup(), new FunGroup(), new GameStatsGroup(), new CurrencyGroup(), new SettingGroup(),
                new MusicGroup(), new DonatorGroup(), new GameGroup());
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
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests -> applicationService
                        .bulkOverwriteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, requests))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} commands registered", cmdCount))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));
    }

    public Map<String, BaseCmd> getCommands() {
        return this.commandsMap;
    }

    public BaseCmd getCommand(String name) {
        final BaseCmd cmd = this.commandsMap.get(name);
        if (cmd != null) {
            return cmd;
        }

        return this.commandsMap.values().stream()
                .filter(it -> it instanceof BaseCmdGroup)
                .flatMap(it -> ((BaseCmdGroup) it).getSubCommands().stream())
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static CommandManager getInstance() {
        return CommandManager.instance;
    }
}
