package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class FunGroup extends BaseCmdGroup {

    public FunGroup() {
        super(CommandCategory.FUN, "fun", "Fun commands",
                List.of(new ChatCmd(), new JokeCmd(), new ThisDayCmd()));
    }

}
