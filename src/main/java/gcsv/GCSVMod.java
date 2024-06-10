package gcsv;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class GCSVMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gcsv");
    public static GCSVMod INSTANCE;

    public GCSVPropertiesHandler properties;
    public GotoManager gotoManager;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        Path propertyPath = Paths.get("config/gcsv.properties");
        Properties javaProps = GCSVPropertiesHandler.loadProperties(propertyPath);
        properties = new GCSVPropertiesHandler(javaProps);
        properties.saveProperties(propertyPath);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (environment.dedicated) {
                GotoCommand.register(dispatcher);
                GotoAcceptCommand.register(dispatcher);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer) -> gotoManager.close());

        gotoManager = new GotoManager();
    }


}