package com.jgcomptech.tools.pluginsystem;

import javafx.beans.property.SimpleStringProperty;

public class PluginInfo {
    final private SimpleStringProperty name = new SimpleStringProperty("", "name", "");
    final private SimpleStringProperty version = new SimpleStringProperty("", "version", "");
    final private SimpleStringProperty type = new SimpleStringProperty("", "type", "");
    final private SimpleStringProperty author = new SimpleStringProperty("", "author", "");
    final private SimpleStringProperty company = new SimpleStringProperty("", "company", "");
    final private SimpleStringProperty license = new SimpleStringProperty("", "license", "");

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public String getVersion() { return version.get(); }
    public void setVersion(String version) { this.version.set(version); }

    public String getType() { return type.get(); }
    public void setType(String type) { this.type.set(type); }

    public String getAuthor() { return author.get(); }
    public void setAuthor(String author) { this.author.set(author); }

    public String getCompany() { return company.get(); }
    public void setCompany(String company) { this.company.set(company); }

    public String getLicense() { return license.get(); }
    public void setLicense(String license) { this.license.set(license); }

    @Override
    public String toString() { return (name.get() + " " + version.get() + ", by " + company.get()); }
}
