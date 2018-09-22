package me.shadorc.shadbot.api.image.r34;

import com.fasterxml.jackson.annotation.JsonProperty;

public class R34Post {

	@JsonProperty("tags")
	private String tags;
	@JsonProperty("file_url")
	private String fileUrl;
	@JsonProperty("source")
	private String source;
	@JsonProperty("width")
	private int width;
	@JsonProperty("height")
	private int height;
	@JsonProperty("has_children")
	private boolean hasChildren;

	public String getTags() {
		return this.tags;
	}

	public String getFileUrl() {
		return this.fileUrl;
	}

	public String getSource() {
		return this.source;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public boolean hasChildren() {
		return this.hasChildren;
	}

	@Override
	public String toString() {
		return String.format("R34Post [tags=%s, fileUrl=%s, source=%s, width=%s, height=%s, hasChildren=%s]",
				this.tags, this.fileUrl, this.source, this.width, this.height, this.hasChildren);
	}

}
