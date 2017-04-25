package com.jgcomptech.tools.pluginsystem;

import com.jgcomptech.tools.dialogs.MessageBox;
import com.jgcomptech.tools.dialogs.MessageBoxIcon;
import com.jgcomptech.tools.pluginsystem.events.PluginEventGenerator;
import com.jgcomptech.tools.pluginsystem.events.UpdateLoadedEvent;
import com.jgcomptech.tools.pluginsystem.events.UpdateLoadedListener;
import javafx.scene.layout.BorderPane;
import org.openide.util.Lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class PluginManager implements UpdateLoadedListener {
    private static PluginManager instance = null;
    private String errorTitle  = "";
    private String pluginsDirectory = null;
    private PluginManager() { /*Exists only to defeat instantiation.*/ }

    public static synchronized PluginManager getInstance() {
        if(instance == null) {
            instance = new PluginManager();
            PluginEventGenerator.getInstance().addUpdateLoadedListener(instance);
        }
        return instance;
    }

    public HashSet<PluginInstance> getInstalledPlugins() {
        final HashSet<PluginInstance> installedPlugins = new HashSet<>();
        for(final PluginInstance plugin : PluginPool.getInstance().getPlugins()) {
            if(plugin.isInstalled()) installedPlugins.add(plugin);
        }
        return installedPlugins;
    }
    public HashSet<PluginInstance> getLoadedPlugins() { return PluginPool.getInstance().getPlugins(); }

    public String getErrorTitle() { return errorTitle; }
    public void setErrorTitle(String errorTitle) { this.errorTitle = errorTitle; }

    public void initializePluginDirectory(String path, String folderName) throws IOException {
        pluginsDirectory = path + System.getProperty("file.separator") + folderName;
        Files.createDirectories(Paths.get(pluginsDirectory));
    }

    public String getPluginsDirectory() { return pluginsDirectory; }

    public boolean isPluginInstalled(String pluginName) {
        return isPluginLoaded(pluginName) && getLoadedPlugin(pluginName).isInstalled();
    }
    public boolean isPluginLoaded(String pluginName) {
        return PluginPool.getInstance().pluginExists(pluginName) && getLoadedPlugin(pluginName).isPaneLoaded();
    }

    public String getPluginUpdateStatus(String pluginName) {
        return getLoadedPlugin(pluginName).isUpdateNeeded() ? "Needs Update" : "Current";
    }
    public boolean isPluginUpdated(String pluginName) { return getLoadedPlugin(pluginName).isUpdateNeeded(); }
    public String updatePlugin(String pluginName) {
        final PluginInstance plugin = getLoadedPlugin(pluginName);
        if(plugin.isUpdateNeeded()) return "Update Not Needed";
        else {
            final boolean downloadStatus;
            final String downloadURL = plugin.getDownloadURL();
            if(downloadURL != null) {
                //TODO add download code
                downloadStatus = false;
            } else return "Update Failed - Invalid URL";

            return downloadStatus ? "Update Succeeded" : "Update Failed";
        }
    }

    public boolean doesPluginContainStage(String pluginName, LoadStageType stage) {
        return getPluginLoadStages(pluginName) != null && getPluginLoadStages(pluginName).contains(stage);
    }
    public HashSet<LoadStageType> getPluginLoadStages(String pluginName) {
        return filterPluginsSingle((PluginInstance i) -> i.getPluginName().equals(pluginName)).getStages();
    }

    public PluginInstance getLoadedPlugin(String pluginName) { return PluginPool.getInstance().getPlugin(pluginName); }

    public PluginInstance getInstalledPlugin(String pluginName) {
        final PluginInstance plugin = getLoadedPlugin(pluginName);
        if(plugin.isInstalled()) return plugin;
        else throw new IllegalArgumentException("Plugin Not Installed!");
    }

    public PluginInfo getPluginInfo(String pluginName) { return getLoadedPlugin(pluginName).getPluginInfo(); }
    public String getPluginInfoString(String pluginName) {
        final PluginInstance plugin = getLoadedPlugin(pluginName);
        if(plugin.getPluginInfo() != null) {
            final String updateStatus = plugin.isUpdateNeeded() ? "Needs Update" : "Current";
            return plugin.getPluginType() + " " + plugin.getPluginName() + " " + plugin.getPluginVersion() + " by "
                    + plugin.getPluginCompany() + "(" + plugin.getPluginAuthor() + ")" + " - " + updateStatus;
        } else throw new IllegalArgumentException("Plugin Info Not defined!");
    }
    public String getPluginVersion(String pluginName) { return getLoadedPlugin(pluginName).getPluginVersion(); }
    public String getPluginType(String pluginName) { return getLoadedPlugin(pluginName).getPluginVersion(); }
    public String getPluginAuthor(String pluginName) { return getLoadedPlugin(pluginName).getPluginAuthor(); }
    public String getPluginCompany(String pluginName) { return getLoadedPlugin(pluginName).getPluginCompany(); }
    public String getPluginLicense(String pluginName) { return getLoadedPlugin(pluginName).getPluginCompany(); }

    public synchronized Messages.ErrorStatus installPlugin(Object source, String pluginName) {
        final PluginInstance plugin = getLoadedPlugin(pluginName);
        if(isPluginLoaded(pluginName)) {
            if(isPluginInstalled(pluginName)) {
                MessageBox.show("Plugin " + quoteString(pluginName) + " Already Installed!",
                        "Plugin Manager - Error", MessageBoxIcon.ERROR);
                return Messages.ErrorStatus.ALREADY_INSTALLED;
            } else {
                if(plugin.doesContainStage(LoadStageType.NORMAL_LOAD)) {
                    PluginEventGenerator.getInstance().installPlugin(this, pluginName);
                    int num = 0;
                    while(!plugin.isPaneLoaded()) {
                        if(num == 30) break;
                        System.out.println(num);
                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                        num++;
                    }

                    if(plugin.isPaneLoaded()) {
                        try {
                            //Check if Plugin stage loaded successfully
                            if(plugin.loadStage(LoadStageType.NORMAL_LOAD)) plugin.setInstalled(true);
                            else {
                                //Since error message was returned, show message box.
                                if(plugin.getErrorMessage() != null) {
                                    showErrorMessage(pluginName, plugin.getErrorMessage(),
                                            Messages.ErrorStage.INSTALL, Messages.ErrorType.MESSAGE);
                                } else {
                                    showErrorMessage(pluginName,"",
                                            Messages.ErrorStage.INSTALL, Messages.ErrorType.NO_MESSAGE);
                                }

                                //Since error occurred, attempt to unloadPlugin Plugin
                                unloadPlugin(plugin, false);
                            }
                        } catch(Exception e) {
                            //Catches uncaught exceptions when Plugin stage is loaded.
                            showErrorMessage(pluginName, generateErrorString(e),
                                    Messages.ErrorStage.INSTALL, Messages.ErrorType.EXCEPTION);

                            //Since exception occurred, attempt to unloadPlugin Plugin
                            unloadPlugin(plugin, false);
                        }
                    }
                } else {
                    MessageBox.show("Plugin " + quoteString(pluginName) +
                                    " cannot be installed in Normal load stage!",
                            "Plugin Manager - Error", MessageBoxIcon.ERROR);
                    return Messages.ErrorStatus.INVALID_STAGE;
                }
            }
        } else return Messages.ErrorStatus.NOT_LOADED;

        return Messages.ErrorStatus.LOADING_ERROR;
    }
    public synchronized Messages.ErrorStatus uninstallPlugin(Object source, String pluginName) {
        final PluginInstance plugin = getInstalledPlugin(pluginName);
        if(isPluginLoaded(pluginName)) {
            if(isPluginInstalled(pluginName)) {
                final Messages.ErrorStatus result = unloadPlugin(plugin,true);
                if(result == Messages.ErrorStatus.NO_ERROR) {
                    plugin.setInstalled(false);
                    PluginEventGenerator.getInstance().uninstallPlugin(this, pluginName);
                }
                return result;
            } else {return Messages.ErrorStatus.NOT_INSTALLED;}
        } else return Messages.ErrorStatus.NOT_LOADED;
    }
    private synchronized Messages.ErrorStatus unloadPlugin(PluginInstance plugin, boolean isUninstalling) {
        final String pluginName = plugin.getPluginName();
        final Messages.ErrorStage errorStage =
                isUninstalling ? Messages.ErrorStage.UNINSTALL : Messages.ErrorStage.UNLOAD;
        try {
            if(plugin.useUnload()) {
                //Check if Plugin unloaded successfully
                if(plugin.unloadPlugin()) return Messages.ErrorStatus.NO_ERROR;
                else {
                    //Since error message was returned, show message box.
                    if(plugin.getErrorMessage() != null) {
                        showErrorMessage(pluginName, plugin.getErrorMessage(), errorStage, Messages.ErrorType.MESSAGE);
                    } else {
                        showErrorMessage(pluginName,"", errorStage, Messages.ErrorType.NO_MESSAGE);
                    }

                    return Messages.ErrorStatus.UNLOADING_ERROR;
                }
            } return Messages.ErrorStatus.NO_ERROR;

        } catch(Exception e) {
            //Catches uncaught exceptions when Plugin is unloaded.
            showErrorMessage(pluginName, generateErrorString(e), errorStage, Messages.ErrorType.EXCEPTION);

            return Messages.ErrorStatus.UNLOADING_ERROR;
        }
    }

    private static String generateErrorString(Exception e) {
        return "[ " + e.getClass().getCanonicalName() + " ]" + System.getProperty("line.separator") + e.getMessage();
    }

    private void showErrorMessage(String pluginName, String message, Messages.ErrorStage stage, Messages.ErrorType type){
        String text = "";
        String stageName = "";
        boolean showUnhandled = false;

        switch(stage) {
            case INSTALL:
                stageName = "install";
                break;
            case UNINSTALL:
                stageName = "uninstall";
                break;
            case UNLOAD:
                stageName = "unloadPlugin";
                break;
        }

        switch(type) {

            case MESSAGE:
                text = message;
                break;
            case NO_MESSAGE:
                text = "Could not retrieve error message!";
                break;
            case EXCEPTION:
                text = message;
                showUnhandled = true;
                break;
        }

        final String headerText = quoteString(pluginName) +
                " Plugin failed to " + stageName + (showUnhandled ? "!" : "! Uncaught Exception!");

        MessageBox.show(text, errorTitle, headerText, MessageBoxIcon.ERROR);
    }

    public static HashSet<PluginInstance> filterPlugins(Predicate<PluginInstance> p) {
        return PluginPool.getInstance().filterPlugins(p);
    }

    public static PluginInstance filterPluginsSingle(Predicate<PluginInstance> p) {
        return PluginPool.getInstance().filterPluginsSingle(p);
    }

    @Override
    public void UpdateLoadedReceived(UpdateLoadedEvent e) { getLoadedPlugin(e.getPluginName()).setPaneLoaded(true); }

    public static String quoteString(String str) {
        return "\"" + str + "\"";
    }

    public static class PluginPool {
        private static PluginPool instance = null;
        private final HashSet<PluginInstance> plugins = new HashSet<>();
        private final HashMap<String, BorderPane> pluginObjects = new HashMap<>();

        private PluginPool() { /*Exists only to defeat instantiation.*/ }

        public static synchronized PluginPool getInstance() {
            if(instance == null) {
                instance = new PluginPool();
            }
            return instance;
        }

        public HashSet<PluginInstance> getPlugins() { return plugins; }

        public void addPlugin(PluginInstance plugin) {
            boolean pluginExists = false;

            for(final PluginInstance p : plugins) {
                if(p.getPluginName().equals(plugin.getPluginName())) {
                    pluginExists = true;
                }
            }

            if(!pluginExists) {
                plugins.add(plugin);
            }
        }

        public void addPluginObjects(String pluginNavString, BorderPane pane) {
            boolean stringExists = false;

            for(final Map.Entry<String, BorderPane> entry : pluginObjects.entrySet()) {
                if(entry.getKey().equals(pluginNavString)) {
                    stringExists = true;
                }
            }

            if(!stringExists) {
                pluginObjects.put(pluginNavString, pane);
            }
        }

        public void removePlugin(PluginInstance plugin) {
            if(pluginExists(plugin.getPluginName())) {
                removePluginObjects(plugin);
                plugins.remove(plugin);
            }
        }

        public void removePluginObjects(PluginInstance plugin) {
            if(pluginObjects.containsKey(plugin.getPluginName())) {
                pluginObjects.remove(plugin.getPluginName());
            }
        }

        public String getPluginNavString(String pluginName) {
            for(final Map.Entry<String, BorderPane> entry : pluginObjects.entrySet()) {
                if(entry.getKey().equals(pluginName)) {
                    return entry.getKey();
                }
            }

            throw new IllegalArgumentException("Plugin Not Found!");
        }

        public BorderPane getPluginBorderPane(String pluginName) {
            for(final Map.Entry<String, BorderPane> entry : pluginObjects.entrySet()) {
                if(entry.getKey().equals(pluginName)) {
                    return entry.getValue();
                }
            }

            throw new IllegalArgumentException("Plugin Not Found!");
        }

        public boolean pluginExists(String pluginName) {
            return filterPluginsSingle((PluginInstance i) -> i.getPluginName().equals(pluginName)) != null;
        }

        public PluginInstance getPlugin(String pluginName) {
            return filterPluginsSingle((PluginInstance i) -> i.getPluginName().equals(pluginName));
        }

        public PluginInstance filterPluginsSingle(Predicate<PluginInstance> p) {
            for (final PluginInstance plugin : plugins) {
                if (p.test(plugin)) {
                    return plugin;
                }
            }
            throw new IllegalArgumentException("Plugin Not Found!");
        }

        public HashSet<PluginInstance> filterPlugins(Predicate<PluginInstance> p) {
            final HashSet<PluginInstance> result = new HashSet<>();
            for (final PluginInstance plugin : plugins) {
                if (p.test(plugin)) {
                    result.add(plugin);
                }
            }
            return result;
        }
    }

    public static class PluginLoader {
        private static PluginLoader instance = null;

        private PluginLoader() { /*Exists only to defeat instantiation.*/ }

        public static synchronized PluginLoader getInstance() {
            if(instance == null) {
                instance = new PluginLoader();
            }
            return instance;
        }

        public synchronized boolean loadInternalPlugins() {
            final Collection<? extends IPlugin> plugins = Lookup.getDefault().lookupAll(IPlugin.class);
            for(final IPlugin plugin : plugins) { loadPlugin(new PluginInstance(plugin)); }
            return true;
        }

        private synchronized boolean loadExternalPlugins()
                throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
            final HashSet<PluginInstance> plugins = searchExternalPlugins(PluginManager.instance.getPluginsDirectory());
            for(final PluginInstance plugin : plugins) { loadPlugin(plugin); }
            return true;
        }
        public HashSet<PluginInstance> getLoadedPluginsByStage(LoadStageType stage) {
            return filterPlugins((PluginInstance i) -> i.getStages().contains(stage));
        }

        private HashSet<PluginInstance> searchExternalPlugins(String directory)
                throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
            final HashSet<PluginInstance> IPluginCollection = new HashSet<>(5);
            final File dir = new File(directory);
            //If specified path is a file, throw exception
            if(dir.isFile()) {
                throw new IllegalArgumentException(quoteString(directory) + " must be a directory, not a file.");
            }
            //Get a list of all jar files in directory
            final File[] files = dir.listFiles((dir1, name) -> (name.endsWith(".jar")));

            //Returns an empty collection if directory is empty
            if(files == null) { return IPluginCollection; }

            //Look through jar files in directory
            for(final File file : files) {
                //Get name of jar file
                final String jarPath = file.getAbsolutePath();

                //Add URL of the jar file to system class loader
                final URL fileURL = file.toURI().toURL();

                boolean isURLAlreadyInClassPath = false;

                final URLClassLoader sysClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                final URL[] urls = sysClassLoader.getURLs();

                //Check if url already exists
                for(final URL url : urls) {
                    if(url.toString().equalsIgnoreCase(fileURL.toString())) { isURLAlreadyInClassPath = true; }
                }

                if(!isURLAlreadyInClassPath) {
                    try {
                        //Save the addURL method to an object that can be invoked
                        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                        method.setAccessible(true);
                        method.invoke(ClassLoader.getSystemClassLoader(), fileURL);
                    } catch(Throwable t) {
                        final IllegalStateException exception =
                                new IllegalStateException("Error, could not add URL to system classloader");
                        exception.setStackTrace(t.getStackTrace());
                        throw exception;
                    }
                }

                //A list of classes in the jar file
                final List<String> classNames = new ArrayList<>(10);

                try(JarInputStream jarFile = new JarInputStream(new FileInputStream(jarPath))) {
                    JarEntry jarEntry;
                    while(true) {
                        //Look through all class files in jar file
                        jarEntry = jarFile.getNextJarEntry();
                        final Manifest man = jarFile.getManifest();
                        //If no more files are found, exit loop
                        if(jarEntry == null) { break; }
                        //Check if file is a class file
                        if(jarEntry.getName().endsWith(".class")) {
                            //If the file is a class file, get the class name
                            final String className = jarEntry.getName().replaceAll("/", "\\.");
                            //Add class to list
                            classNames.add(className);
                        }
                    }
                }

                try(URLClassLoader classLoader = new URLClassLoader(new URL[] {
                        new File("jar:file://" + jarPath + "!/").toURI().toURL()
                })) {
                    //Look through all classes in jar file
                    for(final String className : classNames) {
                        // Remove the ".class" file extension
                        final String name = className.substring(0, className.length() - 6);

                        //Create an object to hold the current class
                        final Class classObject = classLoader.loadClass(name);

                        if(classObject != null) {
                            if(classObject.isInterface()) break;
                            final List<Class> interfaces =
                                    new ArrayList<>(Arrays.asList(classObject.getInterfaces()));
                            if(!interfaces.isEmpty()) {
                                for(final Class c : interfaces) {
                                    // Implement the IPlugin interface
                                    if(c.getName().equals(IPlugin.class.getTypeName())) {
                                        //If class implements IPlugin, add class to list
                                        final IPlugin plugin = (IPlugin) classObject.newInstance();
                                        IPluginCollection.add(new PluginInstance(plugin));
                                    } else {
                                        final List<Class> interfaces2 =
                                                new ArrayList<>(Arrays.asList(c.getInterfaces()));
                                        if(!interfaces2.isEmpty()) {
                                            for(final Class c2 : interfaces2) {
                                                // Implement the IPlugin interface
                                                if(c2.getName().equals(IPlugin.class.getTypeName())) {
                                                    //If class implements IPlugin, add class to list
                                                    final IPlugin plugin = (IPlugin) classObject.newInstance();
                                                    IPluginCollection.add(new PluginInstance(plugin));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return IPluginCollection;
        }

        private void loadPlugin(PluginInstance plugin) { plugin.loadPlugin(); }
    }
}
