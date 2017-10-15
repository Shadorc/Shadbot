package me.shadorc.discordbot;

public class Version {

	private final int major, minor, revision;

	public Version(int major, int minor, int revision) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + revision;
	}
}
