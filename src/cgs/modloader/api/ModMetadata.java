package cgs.modloader.api;

public class ModMetadata {
    public String id = "";
    public String name = "";
    public String version = "1.0.0";
    public String author = "Unknown";
    public String description = "";
    public String entrypoint = "";
    public String icon = "";
    public boolean enabled = true;
    public boolean builtin = false;

    public ModMetadata() {}

    public ModMetadata(String id, String name, String version, String author, String description, String entrypoint) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.entrypoint = entrypoint;
        this.enabled = true;
    }
}
