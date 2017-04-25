package com.jgcomptech.tools.pluginsystem;

import javafx.fxml.FXMLLoader;

public interface IPlugin {
    PluginInfo getPluginInfo();

    boolean unloadPlugin();
    boolean useUnload();

    boolean loadPreStage();
    boolean loadNormalStage();
    boolean loadPostStage();

    FXMLLoader getFXMLLoader();
    String getFXMLPath();

    boolean hasError();
    String getErrorMessage();

    boolean isUpdateNeeded();
    String getDownloadURL();

    void setMainAppIconPath(String path);
    String getMainAppIconPath();
}