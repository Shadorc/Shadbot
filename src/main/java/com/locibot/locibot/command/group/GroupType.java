package com.locibot.locibot.command.group;

public enum GroupType {
    DEFAULT("Test", "https://img.icons8.com/ios-filled/344/placeholder-thumbnail-xml.png"), //DB-TeamType = 0
    CLASH("clash", "https://img.icons8.com/doodle/344/league-of-legends.png"),              //DB-TeamType = 1
    AMONGUS("AmongUs", "https://img.icons8.com/doodle/344/among-us.png"),                   //DB-TeamType = 2
    VALORANT("Valorant", "https://img.icons8.com/doodle/344/valorant.png");                 //DB-TeamType = 3

    private final String name;
    private final String url;

    GroupType(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return url;
    }
}
