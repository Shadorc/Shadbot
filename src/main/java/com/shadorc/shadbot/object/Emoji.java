package com.shadorc.shadbot.object;

public enum Emoji {

    CHECK_MARK("white_check_mark"),
    WARNING("warning"),
    ACCESS_DENIED("no_entry_sign"),
    RED_CROSS("x"),
    LOCK("lock"),
    GREY_EXCLAMATION("grey_exclamation"),
    RED_EXCLAMATION("exclamation"),
    QUESTION("question"),
    RED_FLAG("triangular_flag_on_post"),
    INFO("information_source"),
    MAGNIFYING_GLASS("mag"),
    STOPWATCH("stopwatch"),
    GEAR("gear"),
    HOURGLASS("hourglass_flowing_sand"),
    PURSE("purse"),
    MONEY_BAG("moneybag"),
    BANK("bank"),
    DICE("game_die"),
    TICKET("tickets"),
    SPADES("spades"),
    CLUBS("clubs"),
    HEARTS("hearts"),
    DIAMONDS("diamonds"),
    THERMOMETER("thermometer"),
    CLOUD("cloud"),
    WIND("wind_blowing_face"),
    RAIN("cloud_rain"),
    DROPLET("droplet"),
    MUSICAL_NOTE("musical_note"),
    TRACK_NEXT("track_next"),
    PLAY("arrow_forward"),
    PAUSE("pause_button"),
    REPEAT("repeat"),
    SOUND("sound"),
    MUTE("mute"),
    STOP_BUTTON("stop_button"),
    THUMBSDOWN("thumbsdown"),
    SPEECH("speech_balloon"),
    CLAP("clap"),
    TRIANGULAR_RULER("triangular_ruler"),
    SCISSORS("scissors"),
    GEM("gem"),
    LEAF("leaves"),
    BALLOT_BOX("ballot_box"),
    BUG("bug"),
    ROCKET("rocket"),
    BROKEN_HEART("broken_heart"),
    ID("id"),
    CROWN("crown"),
    MAP("map"),
    BIRTHDAY("birthday"),
    BUSTS_IN_SILHOUETTE("busts_in_silhouette"),
    MICROPHONE("microphone2"),
    KEYBOARD("keyboard"),
    SPEECH_BALLOON("speech_balloon"),
    DATE("date"),
    BUST_IN_SILHOUETTE("bust_in_silhouette"),
    MILITARY_MEDAL("military_medal"),
    ROBOT("robot"),
    SATELLITE("satellite"),
    SCREWDRIVER("screwdriver"),
    APPLE("apple"),
    CHERRIES("cherries"),
    BELL("bell"),
    GIFT("gift");

    private final String discordNotation;

    Emoji(String discordNotation) {
        this.discordNotation = discordNotation;
    }

    @Override
    public String toString() {
        return ":%s:".formatted(this.discordNotation);
    }
}
