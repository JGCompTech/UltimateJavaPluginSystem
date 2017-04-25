package com.jgcomptech.tools.pluginsystem;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(LoadStages.class)
@Documented
public @interface LoadStage {
    LoadStageType stage();
    boolean active() default true;
}