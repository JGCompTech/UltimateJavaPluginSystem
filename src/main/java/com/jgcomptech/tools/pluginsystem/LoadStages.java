package com.jgcomptech.tools.pluginsystem;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface LoadStages {
    LoadStage[] value();
}