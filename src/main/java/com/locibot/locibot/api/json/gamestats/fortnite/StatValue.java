package com.locibot.locibot.api.json.gamestats.fortnite;

public record StatValue(int valueInt, double valueDec) {

    @Override
    public String toString() {
        if (this.valueDec != 0) {
            return Double.toString(this.valueDec);
        }
        return Integer.toString(this.valueInt);
    }

}
