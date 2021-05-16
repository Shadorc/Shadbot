package com.shadorc.shadbot.service.task;

import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

public interface Task {

    boolean isEnabled();

    Disposable schedule(Scheduler scheduler);

}
