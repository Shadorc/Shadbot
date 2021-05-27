package com.locibot.locibot.command.group;

public enum GroupType {
    DEFAULT("Test", "https://img.icons8.com/ios-filled/344/placeholder-thumbnail-xml.png", false), //DB-TeamType = 0
    CLASH("clash", "https://img.icons8.com/doodle/344/league-of-legends.png", false),              //DB-TeamType = 1
    AMONGUS("AmongUs", "https://img.icons8.com/doodle/344/among-us.png", false),                   //DB-TeamType = 2
    VALORANT("Valorant", "https://img.icons8.com/doodle/344/valorant.png", true);                 //DB-TeamType = 3

    private final String name;
    private final String url;
    private final boolean inviteOptional;

    GroupType(String name, String url, boolean inviteOptional) {
        this.name = name;
        this.url = url;
        this.inviteOptional = inviteOptional;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isInviteOptional() {
        return inviteOptional;
    }
}
