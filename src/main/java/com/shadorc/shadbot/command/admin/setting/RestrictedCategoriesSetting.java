package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RestrictedCategoriesSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public RestrictedCategoriesSetting() {
        super(List.of("restricted_categories", "restricted-categories", "restricted_category", "restricted-category"),
                Setting.RESTRICTED_CATEGORIES, "Restrict categories to specific channels.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(4);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        final CommandCategory category = Utils.parseEnum(CommandCategory.class, args.get(2),
                new CommandException(String.format("`%s` is not a valid category. %s",
                        args.get(2), FormatUtils.options(CommandCategory.class))));

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractChannels(guild, args.get(3)))
                .collectList()
                .flatMap(mentionedChannels -> {
                    if (mentionedChannels.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Channel `%s` not found.", args.get(3))));
                    }

                    return Mono.zip(Mono.just(mentionedChannels.get(0)),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .flatMap(tuple -> {
                    final GuildChannel mentionedChannel = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<CommandCategory>> restrictedCategories = dbGuild.getSettings()
                            .getRestrictedCategories();
                    switch (action) {
                        case ADD:
                            restrictedCategories.computeIfAbsent(mentionedChannel.getId(),
                                    ignored -> EnumSet.noneOf(CommandCategory.class))
                                    .add(category);
                            strBuilder.append(
                                    String.format("The command category `%s` can now be only used in channel **#%s**.",
                                            category, mentionedChannel.getName()));
                            break;
                        case REMOVE:
                            if (restrictedCategories.containsKey(mentionedChannel.getId())) {
                                restrictedCategories.get(mentionedChannel.getId()).remove(category);
                            }
                            strBuilder.append(
                                    String.format("The command category `%s` can now be used everywhere.", category));
                            break;
                        default:
                            throw new IllegalStateException(String.format("Unknown action: %s", action));
                    }

                    final Map<String, Set<String>> setting = restrictedCategories
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().asString(),
                                    entry -> entry.getValue().stream()
                                            .map(Object::toString)
                                            .collect(Collectors.toSet())));

                    return dbGuild.setSetting(Setting.RESTRICTED_CATEGORIES, setting)
                            .and(context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(Emoji.CHECK_MARK + " " + strBuilder, channel)));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <category> <channel>`",
                        context.getPrefix(), context.getCommandName()), false)
                        .addField("Argument",
                                String.format("**action** - %s", FormatUtils.format(Action.class, "/"))
                                        + String.format("%n**category** - %s", FormatUtils.format(CommandCategory.class, "/"))
                                        + String.format("%n**channel** - the channel to %s", FormatUtils.format(Action.class, "/")),
                                false)
                        .addField("Example", String.format("`%s%s add music #music`",
                                context.getPrefix(), this.getCommandName()), false));
    }
}
