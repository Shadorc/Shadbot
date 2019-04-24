package me.shadorc.shadbot.api.musixmatch;

import discord4j.core.object.Embed;
import me.shadorc.shadbot.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

public class Musixmatch {

	private static final int MAX_LYRICS_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;

	private final Document document;

	public Musixmatch(Document document) {
		this.document = document;
	}

	public String getArtist() {
		return this.document.getElementsByClass("mxm-track-title__artist").text();
	}

	public String getTitle() {
		return StringUtils.remove(this.document.getElementsByClass("mxm-track-title__track ").text(), "Lyrics");
	}

	public String getImageUrl() {
		return "https:" + this.document.getElementsByClass("banner-album-image").select("img").first().attr("src");
	}

	public String getLyrics() {
		return StringUtils.abbreviate(
				NetUtils.br2nl(this.document.getElementsByClass("mxm-lyrics__content ").html()), MAX_LYRICS_LENGTH);
	}

}
