package com.jgcomptech.tools.pluginsystem;

public class Messages {
    enum ErrorType { MESSAGE, NO_MESSAGE, EXCEPTION, }
    enum ErrorStage { INSTALL, UNINSTALL, UNLOAD }

    enum ErrorStatus {
        ALREADY_INSTALLED, NOT_INSTALLED,
        ALREADY_LOADED, NOT_LOADED,
        LOADING_ERROR, UNLOADING_ERROR,
        INVALID_STAGE,
        NO_ERROR
    }
}
