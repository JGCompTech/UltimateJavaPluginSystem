package com.jgcomptech.tools.pluginsystem.events;

import java.util.EventObject;

public class PluginEvent extends EventObject {
    private final String _pluginName;

    PluginEvent(Object source, String pluginName) {
        super(source);
        _pluginName = pluginName;
    }

    public String getPluginName() { return _pluginName; }
}
