package gcsv;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.dedicated.AbstractPropertiesHandler;

import java.util.Properties;

public class GCSVPropertiesHandler extends AbstractPropertiesHandler<GCSVPropertiesHandler> {
    public final int deathItemTicks = this.getInt("death-item-ticks",  4 * 60 * 60 * 20);
    public final int gotoTimeoutSecs = this.getInt("goto-timeout-secs", 15);

    public GCSVPropertiesHandler(Properties properties) {
        super(properties);
    }

    @Override
    protected GCSVPropertiesHandler create(DynamicRegistryManager registryManager, Properties properties) {
        return new GCSVPropertiesHandler(properties);
    }
}
