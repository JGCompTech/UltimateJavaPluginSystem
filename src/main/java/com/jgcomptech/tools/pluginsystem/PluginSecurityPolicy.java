package com.jgcomptech.tools.pluginsystem;

import java.security.*;

public class PluginSecurityPolicy extends Policy {
    /*@Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return isPlugin(domain) ? pluginPermissions() : applicationPermissions();
    }

    private boolean isPlugin(ProtectionDomain domain) {
        return domain.getClassLoader() instanceof PluginClassLoader;
    }

    private PermissionCollection pluginPermissions() {
        final Permissions permissions = new Permissions(); // No permissions
        return permissions;
    }

    private PermissionCollection applicationPermissions() {
        final Permissions permissions = new Permissions();
        permissions.add(new AllPermission());
        return permissions;
    }*/
}
