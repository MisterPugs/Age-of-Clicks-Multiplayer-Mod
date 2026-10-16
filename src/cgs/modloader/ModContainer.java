package cgs.modloader;

import cgs.modloader.api.IMod;
import cgs.modloader.api.ModContext;
import cgs.modloader.api.ModMetadata;
import java.io.File;

public class ModContainer {
    public final ModMetadata metadata;
    public final File location;
    public final IMod instance;
    public final ModContext context;

    public ModContainer(ModMetadata metadata, File location, IMod instance, ModContext context) {
        this.metadata = metadata;
        this.location = location;
        this.instance = instance;
        this.context = context;
    }
}
