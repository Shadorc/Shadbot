package me.shadorc.discordbot;

public class Version {

	private final int major, minor, revision;
	private final boolean isBeta;

	public Version(int major, int minor, int revision, boolean isBeta) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
		this.isBeta = isBeta;
	}

	public boolean isBeta() {
		return isBeta;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + revision + (isBeta ? "-Beta" : "");
	}

}
