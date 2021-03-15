package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Comparator;

public class LeaderboardCmd extends BaseCmd {

    private static final int USER_COUNT = 10;

    public LeaderboardCmd() {
        super(CommandCategory.CURRENCY, "leaderboard", "Show coins leaderboard for this server");
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .flatMapIterable(DBGuild::getMembers)
                .filter(dbMember -> dbMember.getCoins() > 0)
                .sort(Comparator.comparingLong(DBMember::getCoins).reversed())
                .take(USER_COUNT)
                .flatMapSequential(dbMember -> Mono.zip(
                        context.getClient().getUserById(dbMember.getId()).map(User::getUsername),
                        Mono.just(dbMember.getCoins())))
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return "\nEveryone is poor here.";
                    }
                    return FormatUtil.numberedList(USER_COUNT, list.size(),
                            count -> {
                                final Tuple2<String, Long> tuple = list.get(count - 1);
                                return String.format("%d. **%s** - %s",
                                        count, tuple.getT1(), FormatUtil.coins(tuple.getT2()));
                            });
                })
                .map(description -> ShadbotUtil.getDefaultEmbed(
                        embed -> embed.setAuthor("Leaderboard", null, context.getAuthorAvatar())
                                .setDescription(description)))
                .flatMap(context::reply);
    }

}
