package gcsv;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class GotoPlayerConfig implements Serializable {
    private static class ConfigEntry {
        GotoMode defaultGotoMode;

        static class PlayerRelation {
            UUID uuid;
            GotoMode gotoMode;
            long lastUpdate;
        }

        public ArrayList<PlayerRelation> relations;
    }

    private Hashtable<UUID, ConfigEntry> playerConfigs;

    public GotoMode GetRelation(UUID recipient, UUID subject) {
        ConfigEntry cfg = playerConfigs.get(recipient);
        if (cfg == null)
            return GotoMode.ASK;

        // could have a map but I'm not exactly planning on using this for the official picadilly circus mc server
        for (ConfigEntry.PlayerRelation relation : cfg.relations)
            if (relation.uuid.equals(subject))
                return relation.gotoMode;

        return cfg.defaultGotoMode;
    }

    public void setDefault(UUID recipient, GotoMode gotoMode) {
        assert gotoMode != GotoMode.NONE;

        ConfigEntry cfg = playerConfigs.get(recipient);
        if (cfg != null) {
            cfg.defaultGotoMode = gotoMode;
        }

        if (gotoMode != GotoMode.ASK)
        {
            cfg = new ConfigEntry();
            cfg.relations = new ArrayList<>();
            cfg.defaultGotoMode = gotoMode;
            playerConfigs.put(recipient, cfg);
        }
    }

    public void SetRelation(UUID recipient, UUID subject, GotoMode gotoMode) {
        ConfigEntry cfg = playerConfigs.get(recipient);
        if (cfg == null) {
            if (gotoMode == GotoMode.NONE)
                return;

            cfg = new ConfigEntry();
            cfg.defaultGotoMode = GotoMode.ASK;
            playerConfigs.put(recipient, cfg);
        }

        if (cfg.relations == null) {
            if (gotoMode == GotoMode.NONE)
                return;

            cfg.relations = new ArrayList<>();
        }

        final long now = Instant.now().getEpochSecond();

        for (int i = 0; i < cfg.relations.size(); ++i) {
            ConfigEntry.PlayerRelation relation = cfg.relations.get(i);
            if (relation.uuid.equals(subject)) {
                if (gotoMode == GotoMode.NONE) {
                    cfg.relations.remove(i);
                    return;
                }

                relation.gotoMode = gotoMode;
                relation.lastUpdate = now;
                return;
            }
        }

        ConfigEntry.PlayerRelation newRelation = new ConfigEntry.PlayerRelation();
        newRelation.uuid = subject;
        newRelation.gotoMode = gotoMode;
        newRelation.lastUpdate = now;
        cfg.relations.add(newRelation);
    }

    void printInfo(ServerCommandSource commandSource, UUID to) {
        ConfigEntry cfg = playerConfigs.get(to);
        if (cfg == null) {
            commandSource.sendFeedback(() -> Text.literal("No goto config available"), false);
            return;
        }

        commandSource.sendFeedback(() -> Text.literal(String.format("Default response is %s", cfg.defaultGotoMode.toString())), false);
        for (ConfigEntry.PlayerRelation relation : cfg.relations) {
            ServerPlayerEntity player = commandSource.getServer().getPlayerManager().getPlayer(relation.uuid);

            if (player != null)
                commandSource.sendFeedback(() -> Text.literal(String.format("  %s: %s", player.getName().getString(), relation.gotoMode.toString())), false);
        }
    }

    public boolean writeToDisk() {
        try (FileOutputStream fileOutStream = new FileOutputStream("goto-data.bin_new")) {
            DataOutputStream outStream = new DataOutputStream(fileOutStream);
            outStream.writeInt(1);
            outStream.writeInt(playerConfigs.size());

            for (Map.Entry<UUID, ConfigEntry> cfg : playerConfigs.entrySet()) {
                outStream.writeLong(cfg.getKey().getMostSignificantBits());
                outStream.writeLong(cfg.getKey().getLeastSignificantBits());
                outStream.writeByte(cfg.getValue().defaultGotoMode.id);
                outStream.writeInt(cfg.getValue().relations.size());
                for (ConfigEntry.PlayerRelation relation : cfg.getValue().relations) {
                    outStream.writeLong(relation.uuid.getMostSignificantBits());
                    outStream.writeLong(relation.uuid.getLeastSignificantBits());
                    outStream.writeLong(relation.lastUpdate);
                    outStream.writeByte(relation.gotoMode.id);
                }
            }

            fileOutStream.close();

            Files.move(Paths.get("goto_relations.bin_new"), Paths.get("goto-data.bin"), StandardCopyOption.REPLACE_EXISTING);

            return true;

        } catch (IOException e) {
            GCSVMod.LOGGER.warn("IOException encountered during goto table save - {}", e.toString());
        }

        return false;
    }

    public boolean readFromDisk() {
        try (FileInputStream fileInStream = new FileInputStream("goto_relations.bin")) {
            DataInputStream inStream = new DataInputStream(fileInStream);

            Supplier<UUID> readUuid = () -> {
                try {
                    final long upper = inStream.readLong();
                    final long lower = inStream.readLong();
                    return new UUID(upper, lower);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            playerConfigs = new Hashtable<>();

            inStream.readInt(); // version
            final int numConfigs = inStream.readInt();

            for (int i = 0; i < numConfigs; ++i) {
                final ConfigEntry cfg = new ConfigEntry();
                playerConfigs.put(readUuid.get(), cfg);

                cfg.defaultGotoMode = GotoMode.fromId(inStream.readByte());

                final int numRelations = inStream.readInt();
                cfg.relations = new ArrayList<>(numRelations);

                for (int j = 0; j < numRelations; ++j) {
                    ConfigEntry.PlayerRelation relation = new ConfigEntry.PlayerRelation();
                    relation.uuid = readUuid.get();
                    relation.lastUpdate = inStream.readLong();
                    relation.gotoMode = GotoMode.fromId(inStream.readByte());
                    cfg.relations.add(relation);
                }
            }

            fileInStream.close();

            return true;

        } catch (FileNotFoundException e) {
            playerConfigs = new Hashtable<>();
            return true;

        } catch (IOException e) {
            GCSVMod.LOGGER.warn("IOException encountered during goto table read - {}", e.toString());
        }

        return false;
    }
}
