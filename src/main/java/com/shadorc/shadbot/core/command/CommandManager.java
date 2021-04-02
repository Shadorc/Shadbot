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

    private static final Map<String, BaseCmd> COMMANDS_MAP;

    static {
        COMMANDS_MAP = CommandManager.initialize(
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

    public static Mono<Long> register(ApplicationService applicationService, long applicationId) {
        return Flux.fromIterable(COMMANDS_MAP.values())
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests -> applicationService
                        .bulkOverwriteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, requests))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} commands registered", cmdCount))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));
    }

    public static Map<String, BaseCmd> getCommands() {
        return COMMANDS_MAP;
    }

    public static BaseCmd getCommand(String name) {
        return COMMANDS_MAP.get(name);
    }
}
