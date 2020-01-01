package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.image.wallhaven.WallhavenResponse;
import com.shadorc.shadbot.api.image.wallhaven.Wallpaper;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.cli.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WallpaperCmd extends BaseCmd {

    private static final String HOME_URL = "https://wallhaven.cc/api/v1/search";
    private static final String PURITY = "purity";
    private static final String CATEGORY = "category";
    private static final String RATIO = "ratio";
    private static final String RESOLUTION = "resolution";
    private static final String KEYWORD = "keyword";

    private final Options options;

    private enum Purity {
        SFW("100"), SKETCHY("010"), NSFW("001");

        private final String value;

        Purity(String value) {
            this.value = value;
        }

        private String getValue() {
            return this.value;
        }
    }

    private enum Category {
        GENERAL("100"), ANIME("010"), PEOPLE("001");

        private final String value;

        Category(String value) {
            this.value = value;
        }

        private String getValue() {
            return this.value;
        }
    }

    public WallpaperCmd() {
        super(CommandCategory.IMAGE, List.of("wallpaper"), "wp");
        this.setDefaultRateLimiter();

        this.options = new Options();

        this.options.addOption("p", PURITY, true, FormatUtils.format(Purity.class, ", "));
        this.options.addOption("c", CATEGORY, true, FormatUtils.format(Category.class, ", "));

        final Option ratioOption = new Option("rat", RATIO, true, "image ratio");
        this.options.addOption(ratioOption);

        final Option resOption = new Option("res", RESOLUTION, true, "image resolution");
        this.options.addOption(resOption);

        final Option keyOption = new Option("k", KEYWORD, true, KEYWORD);
        this.options.addOption(keyOption);
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading wallpaper...", context.getUsername()))
                .send()
                .then(Mono.fromCallable(() -> {
                    final List<String> args = StringUtils.split(context.getArg().orElse(""));
                    return new DefaultParser().parse(this.options, args.toArray(new String[0]));
                }))
                .onErrorMap(err -> err instanceof UnrecognizedOptionException || err instanceof MissingArgumentException,
                        err -> new CommandException(String.format("%s. Use `%shelp %s` for more information.",
                                err.getMessage(), context.getPrefix(), this.getName())))
                .zipWith(context.isChannelNsfw())
                .flatMap(tuple -> {
                    final CommandLine cmdLine = tuple.getT1();
                    final boolean isNsfw = tuple.getT2();

                    final StringBuilder urlBuilder = new StringBuilder(HOME_URL);
                    urlBuilder.append(String.format("?apikey=%s", Credentials.get(Credential.WALLHAVEN_API_KEY)));

                    if (cmdLine.hasOption(PURITY)) {
                        final Purity purity = this.parseEnum(context, Purity.class, PURITY, cmdLine.getOptionValue(PURITY, Purity.SFW.toString()));
                        if ((purity == Purity.NSFW || purity == Purity.SKETCHY) && !isNsfw) {
                            return Mono.error(new CommandException("Must be NSFW"));
                        }
                        urlBuilder.append(String.format("&purity=%s", purity.getValue()));
                    }

                    if (cmdLine.hasOption(CATEGORY)) {
                        final Category category = this.parseEnum(context, Category.class, CATEGORY, cmdLine.getOptionValue(CATEGORY));
                        urlBuilder.append(String.format("&categories=%s", category.getValue()));
                    }

                    if (cmdLine.hasOption(RATIO)) {
                        urlBuilder.append(String.format("&ratios=%s", cmdLine.getOptionValue(RATIO)));
                    }

                    if (cmdLine.hasOption(RESOLUTION)) {
                        urlBuilder.append(String.format("&resolutions=%s", cmdLine.getOptionValue(RESOLUTION)));
                    }

                    if (cmdLine.hasOption(KEYWORD)) {
                        final String keywords = Arrays.stream(cmdLine.getOptionValue(KEYWORD).split(","))
                                .map(keyword -> String.format("+%s", keyword.trim()))
                                .collect(Collectors.joining());
                        urlBuilder.append(String.format("&q=%s", keywords));
                        urlBuilder.append("&sorting=relevance");
                    } else {
                        urlBuilder.append("&sorting=toplist");
                    }

                    return NetUtils.get(urlBuilder.toString(), WallhavenResponse.class);
                })
                .map(wallhaven -> {
                    final List<Wallpaper> wallpapers = wallhaven.getWallpapers();
                    if (wallpapers.isEmpty()) {
                        return updatableMsg.setContent(
                                String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers were found for the search `%s`",
                                        context.getUsername(), context.getContent()));
                    }

                    final Wallpaper wallpaper = Utils.randValue(wallpapers);
                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Wallpaper", wallpaper.getUrl(), context.getAvatarUrl())
                                    .setImage(wallpaper.getPath())
                                    .addField("Resolution", wallpaper.getResolution(), false)));
                })
                .onErrorResume(err -> "Must be NSFW".equals(err.getMessage()),
                        err -> Mono.just(updatableMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix()))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private <T extends Enum<T>> T parseEnum(Context context, Class<T> enumClass, String name, String value) {
        final T enumObj = Utils.parseEnum(enumClass, value);
        if (enumObj == null) {
            this.throwInvalidArg(context, name);
        }
        return enumObj;
    }

    private void throwInvalidArg(Context context, String name) {
        throw new CommandException(String.format("`%s` value is not valid. Use `%shelp %s` for more information.",
                name, context.getPrefix(), this.getName()));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Search for a wallpaper.")
                .setUsage(String.format("[-p %s] [-c %s] [-rat %s] [-res %s] [-k %s]", PURITY, CATEGORY, RATIO, RESOLUTION, KEYWORD))
                .addArg(PURITY, FormatUtils.format(Purity.class, ", "), true)
                .addArg(CATEGORY, FormatUtils.format(Category.class, ", "), true)
                .addArg(RATIO, "image ratio (e.g. 16x9)", true)
                .addArg(RESOLUTION, "image resolution (e.g. 1920x1080)", true)
                .addArg(KEYWORD, "keywords (e.g. doom,game)", true)
                .setExample(String.format("Search a *SFW* wallpaper in category *Anime*, with a *16x9* ratio :"
                        + "%n`%s%s -p sfw -c anime -rat 16x9`", context.getPrefix(), this.getName()))
                .setSource("https://wallhaven.cc/")
                .build();
    }
}