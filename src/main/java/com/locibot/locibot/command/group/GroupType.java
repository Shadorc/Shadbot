package com.locibot.locibot.command.group;

public enum GroupType {
    DEFAULT("Test", "https://img.icons8.com/ios-filled/344/placeholder-thumbnail-xml.png", false, 1), //DB-TeamType = 0
    CLASH("clash", "https://img.icons8.com/doodle/344/league-of-legends.png", false, 4),              //DB-TeamType = 1
    AMONGUS("AmongUs", "https://img.icons8.com/doodle/344/among-us.png", false, 2),                   //DB-TeamType = 2
    VALORANT("Valorant", "https://img.icons8.com/doodle/344/valorant.png", true, 1);                 //DB-TeamType = 3

    private final String name;
    private final String url;
    private final boolean inviteOptional;
    private final int min_required;

    GroupType(String name, String url, boolean inviteOptional, int min_required) {
        this.name = name;
        this.url = url;
        this.inviteOptional = inviteOptional;
        this.min_required = min_required;
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

    public int getMin_required() {
        return min_required;
    }
}
