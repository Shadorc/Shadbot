package me.shadorc.shadbot.command.image;

import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
import com.ivkos.wallhaven4j.util.exceptions.ConnectionException;
import com.ivkos.wallhaven4j.util.searchquery.SearchQueryBuilder;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "wallpaper" }, alias = "wp")
public class WallpaperCmd extends AbstractCommand {

	private final static String PURITY = "purity";
	private final static String CATEGORY = "category";
	private final static String RATIO = "ratio";
	private final static String RESOLUTION = "resolution";
	private final static String KEYWORD = "keyword";

	private Wallhaven wallhaven;

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		LoadingMessage loadingMsg = new LoadingMessage("Loading wallpaper...", context.getChannel());
		loadingMsg.send();

		if(wallhaven == null) {
			wallhaven = new Wallhaven(APIKeys.get(APIKey.WALLHAVEN_LOGIN), APIKeys.get(APIKey.WALLHAVEN_PASSWORD));
		}

		Options options = new Options();
		options.addOption("p", PURITY, true, FormatUtils.format(Purity.values(), purity -> purity.toString().toLowerCase(), ", "));
		options.addOption("c", CATEGORY, true, FormatUtils.format(Category.values(), cat -> cat.toString().toLowerCase(), ", "));

		Option ratioOpt = new Option("rat", RATIO, true, "image ratio");
		ratioOpt.setValueSeparator('x');
		options.addOption(ratioOpt);

		Option resOpt = new Option("res", RESOLUTION, true, "image resolution");
		resOpt.setValueSeparator('x');
		options.addOption(resOpt);

		Option keyOpt = new Option("k", KEYWORD, true, KEYWORD);
		keyOpt.setValueSeparator(',');
		options.addOption(keyOpt);

		CommandLine cmdLine;
		try {
			List<String> args = StringUtils.split(context.getArg());
			cmdLine = new DefaultParser().parse(options, args.toArray(new String[args.size()]));
		} catch (UnrecognizedOptionException err) {
			loadingMsg.delete();
			throw new IllegalCmdArgumentException(String.format("%s. Use `%shelp %s` for more information.",
					err.getMessage(), context.getPrefix(), this.getName()));
		} catch (ParseException err) {
			loadingMsg.delete();
			Utils.handle("getting a wallpaper", context, err);
			return;
		}

		Purity purity = this.parseEnum(loadingMsg, context, Purity.class, PURITY, cmdLine.getOptionValue(PURITY, Purity.SFW.toString()));
		if((purity.equals(Purity.NSFW) || purity.equals(Purity.SKETCHY)) && !context.getChannel().isNSFW()) {
			loadingMsg.edit(TextUtils.mustBeNSFW(context.getPrefix()));
			return;
		}

		SearchQueryBuilder queryBuilder = new SearchQueryBuilder();
		queryBuilder.purity(purity);

		if(cmdLine.hasOption(CATEGORY)) {
			queryBuilder.categories(this.parseEnum(loadingMsg, context, Category.class, CATEGORY, cmdLine.getOptionValue(CATEGORY)));
		}

		if(cmdLine.hasOption(RATIO)) {
			Dimension dim = this.parseDim(loadingMsg, context, RATIO, cmdLine.getOptionValues(RATIO));
			queryBuilder.ratios(new Ratio((int) dim.getWidth(), (int) dim.getHeight()));
		}

		if(cmdLine.hasOption(RESOLUTION)) {
			Dimension dim = this.parseDim(loadingMsg, context, RESOLUTION, cmdLine.getOptionValues(RESOLUTION));
			queryBuilder.resolutions(new Resolution((int) dim.getWidth(), (int) dim.getHeight()));
		}

		if(cmdLine.hasOption(KEYWORD)) {
			queryBuilder.keywords(cmdLine.getOptionValues(KEYWORD));
		}

		try {
			List<Wallpaper> wallpapers = wallhaven.search(queryBuilder.pages(1).build());
			if(wallpapers.isEmpty()) {
				loadingMsg.edit(TextUtils.noResult(context.getMessage().getContent()));
				return;
			}

			Wallpaper wallpaper = wallpapers.get(ThreadLocalRandom.current().nextInt(wallpapers.size()));

			String tags = FormatUtils.format(wallpaper.getTags(), tag -> String.format("`%s`", StringUtils.remove(tag.toString(), "#")), " ");
			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("Wallpaper")
					.withUrl(wallpaper.getUrl())
					.withImage(wallpaper.getImageUrl())
					.appendField("Resolution", wallpaper.getResolution().toString(), false)
					.appendField("Tags", tags, false);

			loadingMsg.edit(embed.build());
		} catch (ConnectionException err) {
			loadingMsg.delete();
			Utils.handle("getting a wallpaper", context, err.getCause());
		}
	}

	private Dimension parseDim(LoadingMessage msg, Context context, String name, String... values) throws IllegalCmdArgumentException {
		List<String> sizeList = List.of(values);
		if(sizeList.size() != 2) {
			this.throwInvalidArg(msg, context, name);
		}
		Integer width = CastUtils.asPositiveInt(sizeList.get(0));
		Integer height = CastUtils.asPositiveInt(sizeList.get(1));
		if(width == null || height == null) {
			this.throwInvalidArg(msg, context, name);
		}
		return new Dimension(width, height);
	}

	private <T extends Enum<T>> T parseEnum(LoadingMessage msg, Context context, Class<T> enumClass, String name, String value) throws IllegalCmdArgumentException {
		T enumObj = Utils.getValueOrNull(enumClass, value);
		if(enumObj == null) {
			this.throwInvalidArg(msg, context, name);
		}
		return enumObj;
	}

	private void throwInvalidArg(LoadingMessage msg, Context context, String name) throws IllegalCmdArgumentException {
		msg.delete();
		throw new IllegalCmdArgumentException(String.format("`%s` value is not valid. Use `%shelp %s` for more information.",
				name, context.getPrefix(), this.getName()));
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Search for a wallpaper.")
				.setUsage(String.format("[-p %s] [-c %s] [-rat %s] [-res %s] [-k %s]", PURITY, CATEGORY, RATIO, RESOLUTION, KEYWORD))
				.addArg(PURITY, FormatUtils.format(Purity.values(), purity -> purity.toString().toLowerCase(), ", "), true)
				.addArg(CATEGORY, FormatUtils.format(Category.values(), cat -> cat.toString().toLowerCase(), ", "), true)
				.addArg(RATIO, "image ratio (e.g. 16x9)", true)
				.addArg(RESOLUTION, "image resolution (e.g. 1920x1080)", true)
				.addArg(KEYWORD, "keywords (e.g. doom,game)", true)
				.setExample(String.format("Search a *SFW* wallpaper in category *Anime*, with a *16x9* ratio :"
						+ "%n`%s%s -p sfw -c anime -rat 16x9`", prefix, this.getName()))
				.setSource("https://alpha.wallhaven.cc")
				.build();
	}
}