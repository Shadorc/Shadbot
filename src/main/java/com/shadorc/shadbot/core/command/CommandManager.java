package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.currency.CoinsCmd;
import com.shadorc.shadbot.command.currency.LeaderboardCmd;
import com.shadorc.shadbot.command.currency.TransferCoinsCmd;
import com.shadorc.shadbot.command.donator.DonatorGroup;
import com.shadorc.shadbot.command.fun.ChatCmd;
import com.shadorc.shadbot.command.fun.JokeCmd;
import com.shadorc.shadbot.command.fun.ThisDayCmd;
import com.shadorc.shadbot.command.game.GameGroup;
import com.shadorc.shadbot.command.gamestats.GameStatsGroup;
import com.shadorc.shadbot.command.image.ImageGroup;
import com.shadorc.shadbot.command.info.InfoGroup;
import com.shadorc.shadbot.command.moderation.ModerationGroup;
import com.shadorc.shadbot.command.music.*;
import com.shadorc.shadbot.command.owner.OwnerGroup;
import com.shadorc.shadbot.command.setting.SettingGroup;
import com.shadorc.shadbot.command.standalone.*;
import com.shadorc.shadbot.command.util.*;
import com.shadorc.shadbot.command.util.poll.PollCmd;
import com.shadorc.shadbot.command.util.translate.TranslateCmd;
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
                new InfoGroup(), new ImageGroup(), new ModerationGroup(), new OwnerGroup(),
                new GameStatsGroup(), new SettingGroup(),
                new DonatorGroup(), new GameGroup(),
                // Standalone
                new PingCmd(), new HelpCmd(), new AchievementsCmd(), new FeedbackCmd(), new InviteCmd(),
                // Music
                new BackwardCmd(), new BassBoostCmd(), new ClearCmd(), new ForwardCmd(), new NameCmd(),
                new PauseCmd(), new PlaylistCmd(), new RepeatCmd(), new ShuffleCmd(), new SkipCmd(),
                new StopCmd(), new VolumeCmd(), new PlayCmd(),
                // Currency
                new CoinsCmd(), new LeaderboardCmd(), new TransferCoinsCmd(),
                // Fun
                new ChatCmd(), new JokeCmd(), new ThisDayCmd(),
                // Util
                new MathCmd(), new LyricsCmd(), new UrbanCmd(), new WeatherCmd(), new WikipediaCmd(),
                new TranslateCmd(), new PollCmd());
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

    public static Mono<Void> register(ApplicationService applicationService, long applicationId) {
        final Mono<Long> registerGuildCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getCategory() == CommandCategory.OWNER)
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests -> applicationService
                        .bulkOverwriteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, requests))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} guild commands registered (ID: {})",
                        cmdCount, Config.OWNER_GUILD_ID))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        final Mono<Long> registerGlobalCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getCategory() != CommandCategory.OWNER)
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests -> /* TODO: Release applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, requests) */
                        applicationService
                                .bulkOverwriteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, requests))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} global commands registered", cmdCount))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        return registerGuildCommands
                .and(registerGlobalCommands);
    }

    public static Map<String, BaseCmd> getCommands() {
        return COMMANDS_MAP;
    }

    public static BaseCmd getCommand(String name) {
        final BaseCmd cmd = COMMANDS_MAP.get(name);
        if (cmd != null) {
            return cmd;
        }

        return COMMANDS_MAP.values().stream()
                .filter(it -> it instanceof BaseCmdGroup)
                .flatMap(it -> ((BaseCmdGroup) it).getCommands().stream())
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
