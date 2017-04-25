package com.jgcomptech.tools.pluginsystem;

import com.jgcomptech.tools.dialogs.MessageBox;
import com.jgcomptech.tools.dialogs.MessageBoxIcon;

import java.util.HashSet;

public class PluginInstance {
    private IPlugin plugin;
    private final HashSet<LoadStageType> stages = new HashSet<>();
    private boolean isPaneLoaded = false;
    private boolean isInstalled = false;

    private PluginInstance() { /*Exists only to defeat instantiation.*/ }

    public PluginInstance(IPlugin plugin) { this.plugin = plugin; }

    void setPlugin(IPlugin plugin) { this.plugin = plugin; }

    public PluginInfo getPluginInfo() { return plugin.getPluginInfo(); }
    private String getPluginInfo(InfoType type) {
        if(plugin.getPluginInfo() != null) {
            final PluginInfo info = plugin.getPluginInfo();
            switch(type) {
                case NAME:
                    return isNullOrEmpty(info.getName());
                case VERSION:
                    return isNullOrEmpty(info.getVersion());
                case TYPE:
                    return isNullOrEmpty(info.getType());
                case AUTHOR:
                    return isNullOrEmpty(info.getAuthor());
                case COMPANY:
                    return isNullOrEmpty(info.getCompany());
                case LICENSE:
                    return isNullOrEmpty(info.getLicense());
            }
        }
        return "INFO_NOT_DEFINED";
    }

    public String getPluginName() { return getPluginInfo(InfoType.NAME); }
    public String getPluginVersion() { return getPluginInfo(InfoType.VERSION); }
    public String getPluginType() { return getPluginInfo(InfoType.TYPE); }
    public String getPluginAuthor() { return getPluginInfo(InfoType.AUTHOR); }
    public String getPluginCompany() { return getPluginInfo(InfoType.COMPANY); }
    public String getPluginLicense() { return getPluginInfo(InfoType.LICENSE); }

    public HashSet<LoadStageType> getStages() { return stages; }
    public boolean doesContainStage(LoadStageType type) { return stages.contains(LoadStageType.NORMAL_LOAD); }

    public boolean loadStage(LoadStageType type) {
        switch(type) {
            case PRE_LOAD:
                return plugin.loadPreStage();
            case NORMAL_LOAD:
                return plugin.loadNormalStage();
            case POST_LOAD:
                return plugin.loadPostStage();
            //This should never happen
            default: throw new IllegalStateException("Unknown Stage Type!");
        }
    }
    public String getErrorMessage() { return plugin.getErrorMessage(); }
    boolean useUnload() { return plugin.useUnload(); }
    void loadPlugin() {
        final Class pluginClass = plugin.getClass();
        final LoadStage pluginStage = (LoadStage) pluginClass.getAnnotation(LoadStage.class);
        final LoadStage[] pluginStageList = ((LoadStages) pluginClass.getAnnotation(LoadStages.class)).value();

        //Annotation only exists if LoadStage is declared one time
        if(pluginClass.isAnnotationPresent(LoadStage.class)) {
            if(!stages.contains(pluginStage.stage())) {
                stages.add(pluginStage.stage());
            }
        }
        //Annotation only exists if LoadStage is declared more than one time
        else if(pluginClass.isAnnotationPresent(LoadStages.class)) {
            for(final LoadStage stage : pluginStageList) {
                if(stage.active() && !stages.contains(stage.stage())) {
                    stages.add(stage.stage());
                }
            }
        }
        //If annotation is not declared just set to Normal Load
        else stages.add(LoadStageType.NORMAL_LOAD);

        if(getPluginInfo() != null) {
            if(getPluginName() != null && !getPluginName().isEmpty()) {
                PluginManager.PluginPool.getInstance().addPlugin(this);
            } else {
                MessageBox.show("Plugin name not defined!", PluginManager.getInstance().getErrorTitle(),
                        "Plugin failed to load!", MessageBoxIcon.ERROR);
            }
        } else {
            MessageBox.show("Plugin info not defined!", PluginManager.getInstance().getErrorTitle(),
                    "Plugin failed to load!", MessageBoxIcon.ERROR);
        }
    }
    boolean unloadPlugin() {
        final boolean result = plugin.unloadPlugin();
        if(result) PluginManager.PluginPool.getInstance().removePlugin(this);
        return result;
    }

    public boolean isPaneLoaded() { return isPaneLoaded; }
    void setPaneLoaded(boolean value) { isPaneLoaded = value; }
    public boolean isInstalled() { return isInstalled; }
    void setInstalled(boolean value) { isInstalled = value; }
    public boolean isUpdateNeeded() { return plugin.isUpdateNeeded(); }
    public String getDownloadURL() { return plugin.getDownloadURL(); }

    enum InfoType { NAME, VERSION, TYPE, AUTHOR, COMPANY, LICENSE }

    public static String isNullOrEmpty(String input) {
        return input == null || input.isEmpty() ? "NOT_DEFINED" : input;
    }
}
