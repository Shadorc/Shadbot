package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.command.currency.CoinsCmd;
import com.shadorc.shadbot.command.currency.LeaderboardCmd;
import com.shadorc.shadbot.command.currency.TransferCoinsCmd;
import com.shadorc.shadbot.command.donator.DonatorGroupCmd;
import com.shadorc.shadbot.command.fun.ChatCmd;
import com.shadorc.shadbot.command.fun.JokeCmd;
import com.shadorc.shadbot.command.fun.ThisDayCmd;
import com.shadorc.shadbot.command.game.GameGroupCmd;
import com.shadorc.shadbot.command.gamestats.GameStatsGroupCmd;
import com.shadorc.shadbot.command.image.ImageGroupCmd;
import com.shadorc.shadbot.command.image.Rule34Cmd;
import com.shadorc.shadbot.command.info.InfoGroupCmd;
import com.shadorc.shadbot.command.moderation.ModerationGroupCmd;
import com.shadorc.shadbot.command.music.*;
import com.shadorc.shadbot.command.owner.OwnerGroupCmd;
import com.shadorc.shadbot.command.setting.SettingGroupCmd;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class CommandManager {

    private static final Map<String, Cmd> COMMANDS_MAP;

    static {
        COMMANDS_MAP = CommandManager.initialize(
                new InfoGroupCmd(), new ImageGroupCmd(), new ModerationGroupCmd(), new OwnerGroupCmd(),
                new GameStatsGroupCmd(), new SettingGroupCmd(), new DonatorGroupCmd(), new GameGroupCmd(),
                // Image
                new Rule34Cmd(), // TODO Improvement: Add to Image group when Discord autocompletion is implemented
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

    private static Map<String, Cmd> initialize(Cmd... cmds) {
        final Map<String, Cmd> map = new LinkedHashMap<>(cmds.length);
        for (final Cmd cmd : cmds) {
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
                .map(Cmd::asRequest)
                .collectList()
                .flatMapMany(requests -> applicationService
                        .bulkOverwriteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, requests))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} guild commands registered (ID: {})",
                        cmdCount, Config.OWNER_GUILD_ID))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        final Mono<Long> registerGlobalCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getCategory() != CommandCategory.OWNER)
                .map(Cmd::asRequest)
                .collectList()
                .flatMapMany(requests -> applicationService
                        .bulkOverwriteGlobalApplicationCommand(applicationId, requests))
                .count()
                .doOnNext(cmdCount -> DEFAULT_LOGGER.info("{} global commands registered", cmdCount))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        return registerGlobalCommands
                .and(registerGuildCommands);
    }

    public static List<Cmd> getCommands() {
        return COMMANDS_MAP.values().stream()
                .flatMap(it -> {
                    if (it instanceof GroupCmd group) {
                        return group.getSubCommands().stream();
                    }
                    return Stream.of(it);
                })
                .toList();
    }

    public static List<Cmd> getCommands(CommandCategory category) {
        return CommandManager.getCommands().stream()
                .filter(cmd -> cmd.getCategory() == category)
                .toList();
    }

    public static Cmd getCommand(String name) {
        return CommandManager.getCommands().stream()
                .filter(cmd -> {
                    if (cmd instanceof SubCmd subCmd && subCmd.getFullName().equals(name)) {
                        return true;
                    }
                    return cmd.getName().equals(name);
                })
                .findFirst()
                .orElse(null);
    }
}
