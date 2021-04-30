package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.database.guilds.entity.Settings;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.function.Function;

public class SettingShow extends BaseCmd {

    public SettingShow() {
        super(CommandCategory.SETTING, CommandPermission.USER,
                "show", "Show current settings");
    }

    @Override
    public Mono<?> execute(Context context) {
        final Settings settings = context.getDbGuild().getSettings();

        final Set<String> blacklistedCmds = settings.getBlacklistedCmds();
        final Optional<Integer> defaultVolume = settings.getDefaultVol();
        final Optional<Locale> locale = settings.getLocale();
        final Mono<Optional<Tuple2<Channel, String>>> getAutoJoinMessage =
                Mono.justOrEmpty(settings.getAutoJoinMessage())
                        .flatMap(autoMessage -> context.getClient().getChannelById(autoMessage.getChannelId())
                                .zipWith(Mono.just(autoMessage.getMessage())))
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty());
        final Mono<Optional<Tuple2<Channel, String>>> getAutoLeaveMessage =
                Mono.justOrEmpty(settings.getAutoLeaveMessage())
                        .flatMap(autoMessage -> context.getClient().getChannelById(autoMessage.getChannelId())
                                .zipWith(Mono.just(autoMessage.getMessage())))
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty());
        final Mono<List<Role>> getAllowedRoles =
                Flux.fromIterable(settings.getAllowedRoleIds())
                        .flatMap(id -> context.getClient().getRoleById(context.getGuildId(), id))
                        .collectList();
        final Mono<List<Role>> getAutoRoles =
                Flux.fromIterable(settings.getAutoRoleIds())
                        .flatMap(id -> context.getClient().getRoleById(context.getGuildId(), id))
                        .collectList();
        final Mono<Map<Channel, Set<BaseCmd>>> getRestrictedChannels =
                Flux.fromIterable(settings.getRestrictedChannels().entrySet())
                        .flatMap(entry -> context.getClient().getChannelById(entry.getKey())
                                .zipWith(Mono.just(entry.getValue())))
                        .filter(tuple -> !tuple.getT2().isEmpty())
                        .collectMap(Tuple2::getT1, Tuple2::getT2);
        final Mono<Map<Role, Set<BaseCmd>>> getRestrictedRoles =
                Flux.fromIterable(settings.getRestrictedRoles().entrySet())
                        .flatMap(entry -> context.getClient().getRoleById(context.getGuildId(), entry.getKey())
                                .zipWith(Mono.just(entry.getValue())))
                        .filter(tuple -> !tuple.getT2().isEmpty())
                        .collectMap(Tuple2::getT1, Tuple2::getT2);

        return Mono.zip(
                getAllowedRoles,
                getAutoRoles,
                getRestrictedChannels,
                getRestrictedRoles,
                getAutoJoinMessage,
                getAutoLeaveMessage)
                .map(TupleUtils.function((allowedRoles, autoRoles, restrictedChannels, restrictedRoles, autoJoinMessage, autoLeaveMessage) ->
                        ShadbotUtil.getDefaultEmbed(embed -> {
                            embed.setAuthor(context.localize("settings.title"),
                                    "https://github.com/Shadorc/Shadbot/wiki/Settings",
                                    context.getAuthorAvatar());
                            embed.setFooter(context.localize("settings.footer"), null);

                            defaultVolume.ifPresent(volume ->
                                    embed.addField(context.localize("settings.volume"), "%d%%".formatted(volume), false));
                            locale.ifPresent(lang ->
                                    embed.addField("Language", lang.toLanguageTag(), false));
                            autoJoinMessage.ifPresent(it ->
                                    embed.addField(context.localize("settings.auto.join.message"), "%s\n%s"
                                            .formatted(it.getT1().getMention(), it.getT2()), false));
                            autoLeaveMessage.ifPresent(it ->
                                    embed.addField(context.localize("settings.auto.leave.message"), "%s\n%s"
                                            .formatted(it.getT1().getMention(), it.getT2()), false));
                            if (!blacklistedCmds.isEmpty()) {
                                embed.addField(context.localize("settings.blacklist"),
                                        FormatUtil.format(blacklistedCmds, Function.identity(), "\n"), false);
                            }
                            if (!allowedRoles.isEmpty()) {
                                embed.addField(context.localize("settings.allowed.roles"),
                                        FormatUtil.format(allowedRoles, Role::getMention, "\n"), false);
                            }
                            if (!autoRoles.isEmpty()) {
                                embed.addField(context.localize("settings.auto.roles"),
                                        FormatUtil.format(autoRoles, Role::getMention, "\n"), false);
                            }
                            if (!restrictedChannels.isEmpty()) {
                                embed.addField(context.localize("settings.restricted.channels"),
                                        FormatUtil.format(restrictedChannels.entrySet(),
                                                entry -> "%s\n - %s".formatted(
                                                        entry.getKey().getMention(),
                                                        FormatUtil.format(entry.getValue(), BaseCmd::getName, "\n - ")),
                                                "\n"),
                                        false);
                            }
                            if (!restrictedRoles.isEmpty()) {
                                embed.addField(context.localize("settings.restricted.roles"),
                                        FormatUtil.format(restrictedRoles.entrySet(),
                                                entry -> "%s\n - %s".formatted(
                                                        entry.getKey().getMention(),
                                                        FormatUtil.format(entry.getValue(), BaseCmd::getName, "\n - ")),
                                                "\n"),
                                        false);
                            }

                            if (embed.asRequest().fields().toOptional().map(List::isEmpty).orElse(true)) {
                                embed.setDescription(context.localize("settings.none"));
                            }
                        })))
                .flatMap(context::reply);
    }
}
