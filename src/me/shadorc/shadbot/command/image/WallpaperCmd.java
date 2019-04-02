package me.shadorc.shadbot.command.image;

import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import com.ivkos.wallhaven4j.Wallhaven;
import com.ivkos.wallhaven4j.models.misc.Ratio;
import com.ivkos.wallhaven4j.models.misc.Resolution;
import com.ivkos.wallhaven4j.models.misc.enums.Category;
import com.ivkos.wallhaven4j.models.misc.enums.Purity;
import com.ivkos.wallhaven4j.models.wallpaper.Wallpaper;
import com.ivkos.wallhaven4j.util.exceptions.ConnectionException;
import com.ivkos.wallhaven4j.util.exceptions.WallhavenException;
import com.ivkos.wallhaven4j.util.searchquery.SearchQueryBuilder;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class WallpaperCmd extends BaseCmd {

	private static final String PURITY = "purity";
	private static final String CATEGORY = "category";
	private static final String RATIO = "ratio";
	private static final String RESOLUTION = "resolution";
	private static final String KEYWORD = "keyword";

	private final Options options;
	private Wallhaven wallhaven;

	public WallpaperCmd() {
		super(CommandCategory.IMAGE, List.of("wallpaper"), "wp");
		this.setDefaultRateLimiter();

		this.options = new Options();

		this.options.addOption("p", PURITY, true, FormatUtils.format(Purity.class, ", "));
		this.options.addOption("c", CATEGORY, true, FormatUtils.format(Category.class, ", "));

		final Option ratioOption = new Option("rat", RATIO, true, "image ratio");
		ratioOption.setValueSeparator('x');
		this.options.addOption(ratioOption);

		final Option resOption = new Option("res", RESOLUTION, true, "image resolution");
		resOption.setValueSeparator('x');
		this.options.addOption(resOption);

		final Option keyOption = new Option("k", KEYWORD, true, KEYWORD);
		keyOption.setValueSeparator(',');
		this.options.addOption(keyOption);
	}

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return Mono.fromCallable(() -> {
			final List<String> args = StringUtils.split(context.getArg().orElse(""));
			return new DefaultParser().parse(this.options, args.toArray(new String[0]));
		})
				.onErrorMap(err -> err instanceof UnrecognizedOptionException || err instanceof org.apache.commons.cli.MissingArgumentException,
						err -> new CommandException(String.format("%s. Use `%shelp %s` for more information.",
								err.getMessage(), context.getPrefix(), this.getName())))
				.map(cmdLine -> {
					if(this.wallhaven == null) {
						this.wallhaven = new Wallhaven(Credentials.get(Credential.WALLHAVEN_LOGIN),
								Credentials.get(Credential.WALLHAVEN_PASSWORD));
					}
					return cmdLine;
				})
				.zipWith(context.isChannelNsfw())
				.map(tuple -> {
					final CommandLine cmdLine = tuple.getT1();
					final boolean isNsfw = tuple.getT2();

					final Purity purity = this.parseEnum(context, Purity.class, PURITY, cmdLine.getOptionValue(PURITY, Purity.SFW.toString()));
					if((purity.equals(Purity.NSFW) || purity.equals(Purity.SKETCHY)) && !isNsfw) {
						return loadingMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix()));
					}

					final SearchQueryBuilder queryBuilder = new SearchQueryBuilder();
					queryBuilder.purity(purity);

					if(cmdLine.hasOption(CATEGORY)) {
						queryBuilder.categories(this.parseEnum(context, Category.class, CATEGORY, cmdLine.getOptionValue(CATEGORY)));
					}

					if(cmdLine.hasOption(RATIO)) {
						final Dimension dim = this.parseDim(context, RATIO, cmdLine.getOptionValues(RATIO));
						queryBuilder.ratios(new Ratio((int) dim.getWidth(), (int) dim.getHeight()));
					}

					if(cmdLine.hasOption(RESOLUTION)) {
						final Dimension dim = this.parseDim(context, RESOLUTION, cmdLine.getOptionValues(RESOLUTION));
						queryBuilder.resolutions(new Resolution((int) dim.getWidth(), (int) dim.getHeight()));
					}

					if(cmdLine.hasOption(KEYWORD)) {
						queryBuilder.keywords(cmdLine.getOptionValues(KEYWORD));
					}

					final List<Wallpaper> wallpapers = this.wallhaven.search(queryBuilder.pages(1).build());
					if(wallpapers.isEmpty()) {
						return loadingMsg.setContent(
								String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers were found for the search `%s`",
										context.getUsername(), context.getContent()));
					}

					final Wallpaper wallpaper = Utils.randValue(wallpapers);
					final String tags = FormatUtils.format(wallpaper.getTags(),
							tag -> String.format("`%s`", StringUtils.remove(tag.toString(), "#")), " ");

					return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
							.andThen(embed -> embed.setAuthor("Wallpaper", wallpaper.getUrl(), context.getAvatarUrl())
									.setImage(wallpaper.getImageUrl())
									.addField("Resolution", wallpaper.getResolution().toString(), false)
									.addField("Tags", tags, false)));
				})
				.onErrorMap(err -> err instanceof WallhavenException && err.getCause() != null && err.getCause() instanceof ConnectionException,
						err -> new HttpStatusException("Wallhaven is unavailable.", HttpStatus.SC_SERVICE_UNAVAILABLE, "https://alpha.wallhaven.cc/"))
				.flatMap(LoadingMessage::send)
				.doOnTerminate(loadingMsg::stopTyping)
				.then();
	}

	private Dimension parseDim(Context context, String name, String... values) {
		final List<String> sizeList = List.of(values);
		if(sizeList.size() != 2) {
			this.throwInvalidArg(context, name);
		}
		final Integer width = NumberUtils.asPositiveInt(sizeList.get(0));
		final Integer height = NumberUtils.asPositiveInt(sizeList.get(1));
		if(width == null || height == null) {
			this.throwInvalidArg(context, name);
		}
		return new Dimension(width, height);
	}

	private <T extends Enum<T>> T parseEnum(Context context, Class<T> enumClass, String name, String value) {
		final T enumObj = Utils.parseEnum(enumClass, value);
		if(enumObj == null) {
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
				.setSource("https://alpha.wallhaven.cc/")
				.build();
	}
}