package me.dtqdev.bridgeracing.manager;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.Location;
import java.io.File;
import java.io.IOException;

public class SchematicManager {

    private final BridgeRacing plugin;
    private final File schematicsFolder;

    public SchematicManager(BridgeRacing plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    public void paste(String schematicName, Location pasteLocation) throws IOException, DataException, MaxChangedBlocksException {
        File schematicFile = new File(schematicsFolder, schematicName + ".schematic");
        if (!schematicFile.exists()) {
            throw new IOException("Schematic file not found: " + schematicFile.getName());
        }

        SchematicFormat format = SchematicFormat.getFormat(schematicFile);
        CuboidClipboard clipboard = format.load(schematicFile);

        EditSession editSession = new EditSession(new BukkitWorld(pasteLocation.getWorld()), clipboard.getHeight() * clipboard.getWidth() * clipboard.getLength());
        
        clipboard.paste(editSession, new Vector(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()), true);
    }
}