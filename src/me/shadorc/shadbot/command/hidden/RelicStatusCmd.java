// TODO
// package me.shadorc.shadbot.command.hidden;
//
// import java.util.List;
// import java.util.concurrent.TimeUnit;
//
// import discord4j.core.spec.EmbedCreateSpec;
// import me.shadorc.shadbot.Config;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.data.premium.PremiumManager;
// import me.shadorc.shadbot.data.premium.Relic;
// import me.shadorc.shadbot.data.premium.Relic.RelicType;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.TimeUtils;
// import me.shadorc.shadbot.utils.command.Emoji;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import reactor.core.publisher.Mono;
//
// @Command(category = CommandCategory.HIDDEN, names = { "contributor_status", "donator_status", "relic_status" })
// public class RelicStatusCmd extends AbstractCommand {
//
// @Override
// public void execute(Context context) {
// List<Relic> relics = PremiumManager.getRelicsForUser(context.getAuthorId());
// if(relics.isEmpty()) {
// BotUtils.sendMessage(String.format(Emoji.INFO + " You are not a donator. If you like Shadbot, you can help me keep it alive"
// + " by making a donation on **%s**."
// + "%nAll donations are important and really help me %s",
// Config.PATREON_URL, Emoji.HEARTS), context.getChannel());
// return;
// }
//
// context.getAuthorAvatarUrl().subscribe(avatarUrl -> {
// EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
// .setAuthor("Contributor status", null, avatarUrl)
// .setThumbnail("https://orig00.deviantart.net/24e1/f/2015/241/8/7/relic_fragment_by_yukimemories-d97l8c8.png");
//
// for(Relic relic : relics) {
// StringBuilder contentBld = new StringBuilder();
// contentBld.append(String.format("**ID:** %s", relic.getId()));
// if(relic.getType().equals(RelicType.GUILD)) {
// contentBld.append(String.format("%n**Guild ID:** %d", relic.getGuildId().asLong()));
// }
// contentBld.append(String.format("%n**Duration:** %d days", relic.getDuration()));
// if(!relic.isExpired()) {
// long daysLeft = relic.getDuration() - TimeUnit.MILLISECONDS.toDays(TimeUtils.getMillisUntil(relic.getActivationTime()));
// contentBld.append(String.format("%n**Expires in:** %d days", daysLeft));
// }
//
// StringBuilder titleBld = new StringBuilder();
// if(relic.getType().equals(RelicType.GUILD)) {
// titleBld.append("Legendary ");
// }
// titleBld.append(String.format("Relic (%s)", relic.isExpired() ? "Expired" : "Activated"));
//
// embed.addField(titleBld.toString(), contentBld.toString(), false);
// }
//
// BotUtils.sendMessage(embed, context.getChannel());
// });
// }
//
// @Override
// public Mono<EmbedCreateSpec> getHelp(Context context) {
// return new HelpBuilder(this, context)
// .setDescription("Show your contributor status.")
// .build();
// }
// }
