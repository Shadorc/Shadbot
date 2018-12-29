package me.shadorc.shadbot.command.image;

import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.ivkos.wallhaven4j.Wallhaven;
import com.ivkos.wallhaven4j.models.misc.Ratio;
import com.ivkos.wallhaven4j.models.misc.Resolution;
import com.ivkos.wallhaven4j.models.misc.enums.Category;
import com.ivkos.wallhaven4j.models.misc.enums.Purity;
import com.ivkos.wallhaven4j.models.wallpaper.Wallpaper;
import com.ivkos.wallhaven4j.util.exceptions.WallhavenException;
import com.ivkos.wallhaven4j.util.searchquery.SearchQueryBuilder;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "wallpaper" }, alias = "wp")
public class WallpaperCmd extends AbstractCommand {

	private static final String PURITY = "purity";
	private static final String CATEGORY = "category";
	private static final String RATIO = "ratio";
	private static final String RESOLUTION = "resolution";
	private static final String KEYWORD = "keyword";

	private Wallhaven wallhaven;

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		if(this.wallhaven == null) {
			this.wallhaven = new Wallhaven(Credentials.get(Credential.WALLHAVEN_LOGIN),
					Credentials.get(Credential.WALLHAVEN_PASSWORD));
		}

		final Options options = new Options();

		options.addOption("p", PURITY, true, FormatUtils.format(Purity.class, ", "));
		options.addOption("c", CATEGORY, true, FormatUtils.format(Category.class, ", "));

		final Option ratioOption = new Option("rat", RATIO, true, "image ratio");
		ratioOption.setValueSeparator('x');
		options.addOption(ratioOption);

		final Option resOption = new Option("res", RESOLUTION, true, "image resolution");
		resOption.setValueSeparator('x');
		options.addOption(resOption);

		final Option keyOption = new Option("k", KEYWORD, true, KEYWORD);
		keyOption.setValueSeparator(',');
		options.addOption(keyOption);

		CommandLine cmdLine;
		try {
			final List<String> args = StringUtils.split(context.getArg().orElse(""));
			cmdLine = new DefaultParser().parse(options, args.toArray(new String[0]));
		} catch (UnrecognizedOptionException | org.apache.commons.cli.MissingArgumentException err) {
			loadingMsg.stopTyping();
			throw new CommandException(String.format("%s. Use `%shelp %s` for more information.",
					err.getMessage(), context.getPrefix(), this.getName()));
		} catch (final ParseException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}

		return context.isChannelNsfw()
				.flatMap(isNsfw -> {
					final Purity purity = this.parseEnum(loadingMsg, context, Purity.class, PURITY, cmdLine.getOptionValue(PURITY, Purity.SFW.toString()));
					if((purity.equals(Purity.NSFW) || purity.equals(Purity.SKETCHY)) && !isNsfw) {
						return loadingMsg.send(TextUtils.mustBeNsfw(context.getPrefix())).then();
					}

					final SearchQueryBuilder queryBuilder = new SearchQueryBuilder();
					queryBuilder.purity(purity);

					if(cmdLine.hasOption(CATEGORY)) {
						queryBuilder.categories(this.parseEnum(loadingMsg, context, Category.class, CATEGORY, cmdLine.getOptionValue(CATEGORY)));
					}

					if(cmdLine.hasOption(RATIO)) {
						final Dimension dim = this.parseDim(loadingMsg, context, RATIO, cmdLine.getOptionValues(RATIO));
						queryBuilder.ratios(new Ratio((int) dim.getWidth(), (int) dim.getHeight()));
					}

					if(cmdLine.hasOption(RESOLUTION)) {
						final Dimension dim = this.parseDim(loadingMsg, context, RESOLUTION, cmdLine.getOptionValues(RESOLUTION));
						queryBuilder.resolutions(new Resolution((int) dim.getWidth(), (int) dim.getHeight()));
					}

					if(cmdLine.hasOption(KEYWORD)) {
						queryBuilder.keywords(cmdLine.getOptionValues(KEYWORD));
					}

					try {
						final List<Wallpaper> wallpapers = this.wallhaven.search(queryBuilder.pages(1).build());
						if(wallpapers.isEmpty()) {
							return loadingMsg.send(
									String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers were found for the search `%s`",
											context.getUsername(), context.getContent()))
									.then();
						}

						final Wallpaper wallpaper = Utils.randValue(wallpapers);
						final String tags = FormatUtils.format(wallpaper.getTags(),
								tag -> String.format("`%s`", StringUtils.remove(tag.toString(), "#")), " ");

						return context.getAvatarUrl()
								.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
										.setAuthor("Wallpaper", wallpaper.getUrl(), avatarUrl)
										.setImage(wallpaper.getImageUrl())
										.addField("Resolution", wallpaper.getResolution().toString(), false)
										.addField("Tags", tags, false))
								.flatMap(loadingMsg::send)
								.then();
					} catch (final WallhavenException err) {
						loadingMsg.stopTyping();
						throw Exceptions.propagate(Objects.requireNonNullElse(err.getCause(), err));
					}
				});
	}

	private Dimension parseDim(LoadingMessage msg, Context context, String name, String... values) {
		final List<String> sizeList = List.of(values);
		if(sizeList.size() != 2) {
			this.throwInvalidArg(msg, context, name);
		}
		final Integer width = NumberUtils.asPositiveInt(sizeList.get(0));
		final Integer height = NumberUtils.asPositiveInt(sizeList.get(1));
		if(width == null || height == null) {
			this.throwInvalidArg(msg, context, name);
		}
		return new Dimension(width, height);
	}

	private <T extends Enum<T>> T parseEnum(LoadingMessage msg, Context context, Class<T> enumClass, String name, String value) {
		final T enumObj = Utils.getEnum(enumClass, value);
		if(enumObj == null) {
			this.throwInvalidArg(msg, context, name);
		}
		return enumObj;
	}

	private void throwInvalidArg(LoadingMessage loadingMsg, Context context, String name) {
		loadingMsg.stopTyping();
		throw new CommandException(String.format("`%s` value is not valid. Use `%shelp %s` for more information.",
				name, context.getPrefix(), this.getName()));
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
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