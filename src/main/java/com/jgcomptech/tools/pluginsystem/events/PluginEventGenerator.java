package com.jgcomptech.tools.pluginsystem.events;

import java.util.ArrayList;
import java.util.List;


public class PluginEventGenerator {
    private static PluginEventGenerator instance = null;
    private final List<InstallPluginListener> _installListeners = new ArrayList<>();
    private final List<UninstallPluginListener> _uninstallListeners = new ArrayList<>();
    private final List<UpdateLoadedListener> _updateLoadedListeners = new ArrayList<>();

    private PluginEventGenerator() { /*Exists only to defeat instantiation.*/ }

    public static synchronized PluginEventGenerator getInstance() {
        if(instance == null) instance = new PluginEventGenerator();
        return instance;
    }

    public synchronized void installPlugin(Object source, String pluginName) {
        //Create and fire event
        final InstallPluginEvent event = new InstallPluginEvent(this, pluginName);
        for(final Object _listener : _installListeners) {
            ((InstallPluginListener) _listener).InstallRequestReceived(event);
        }
    }

    public synchronized void uninstallPlugin(Object source, String pluginName) {
        //Create and fire event
        final UninstallPluginEvent event = new UninstallPluginEvent(this, pluginName);
        for(final Object _listener : _uninstallListeners) {
            ((UninstallPluginListener) _listener).UninstallRequestReceived(event);
        }
    }

    public synchronized void updatePluginLoaded(Object source, String pluginName) throws IllegalAccessException {
        //Create and fire event
        final UpdateLoadedEvent event = new UpdateLoadedEvent(this, pluginName);
        for(final Object _listener : _updateLoadedListeners) {
            ((UpdateLoadedListener) _listener).UpdateLoadedReceived(event);
        }
    }

    public synchronized void addInstallListener(InstallPluginListener l) { _installListeners.add(l); }
    public synchronized void removeInstallListener(InstallPluginListener l) { _installListeners.remove(l); }
    public synchronized void addUninstallListener(UninstallPluginListener l) { _uninstallListeners.add(l); }
    public synchronized void removeUninstallListener(UninstallPluginListener l) { _uninstallListeners.remove(l); }
    public synchronized void addUpdateLoadedListener(UpdateLoadedListener l) { _updateLoadedListeners.add(l); }
    public synchronized void removeUpdateLoadedListener(UpdateLoadedListener l) { _updateLoadedListeners.remove(l); }
}
