package studio.magemonkey.divinity.testutil;

import org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.compat.NMS;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.mccore.commands.CommandManager;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.fabled.api.player.PlayerData;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockbukkit.mockbukkit.matcher.plugin.PluginManagerFiredEventClassMatcher.hasFiredEventInstance;
import static org.mockbukkit.mockbukkit.matcher.plugin.PluginManagerFiredEventFilterMatcher.hasFiredFilteredEvent;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MockedTest {
    protected ServerMock                server;
    protected CodexEngine               engine;
    protected Divinity                  plugin;
    protected List<PlayerMock>          players          = new ArrayList<>();
    protected Map<UUID, PlayerData>     activePlayerData = new HashMap<>();
    protected MockedStatic<CodexEngine> codexEngine;

    @BeforeAll
    public void setupServer() {
        server = spy(MockBukkit.mock());
        String coreVersion  = System.getProperty("CODEX_VERSION");
        String itemsVersion = System.getProperty("DIVINITY_VERSION");

        try {
            File itemsJar =
                    new File(server.getPluginsFolder().getAbsolutePath(), "Divinity-" + itemsVersion + ".jar");
            if (!itemsJar.exists()) itemsJar.createNewFile();
            createZipArchive(itemsJar, "target/classes");

            File core = DependencyResolver.resolve("studio.magemonkey:codex:" + coreVersion);
            File dest = new File(server.getPluginsFolder().getAbsolutePath(), "CodexCore-" + coreVersion + ".jar");
            FileUtils.copyFile(core, dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        NMS nms = mock(NMS.class);
        when(nms.getVersion()).thenReturn("test");
        when(nms.fixColors(anyString())).thenAnswer(ans -> ans.getArgument(0));

        VersionManager.setNms(nms);

        engine = MockBukkit.load(CodexEngine.class);
        codexEngine = mockStatic(CodexEngine.class);
        codexEngine.when(CodexEngine::get).thenReturn(engine);

        ItemUT.setEngine(engine);

        plugin = MockBukkit.load(Divinity.class);
        server.getScheduler().performOneTick();
//        server.getScheduler().waitAsyncTasksFinished();
    }

    @AfterAll
    public void destroy() {
        plugin.disable();
        CommandManager.unregisterAll();
        MockBukkit.unmock();
        if (codexEngine != null) codexEngine.close();
    }

    @AfterEach
    public void clearData() {
        activePlayerData.clear();
        clearEvents();
        players.clear();
    }

    public PlayerData generatePlayerData(Player player) {
        PlayerData pd = mock(PlayerData.class);
        activePlayerData.put(player.getUniqueId(), pd);

        when(pd.getPlayer()).thenReturn(player);
        return pd;
    }

    public PlayerMock genPlayer(String name) {
        return genPlayer(name, true);
    }

    public PlayerMock genPlayer(String name, boolean op) {
        PlayerMock pm = new PlayerMock(server, name, UUID.randomUUID());
        server.addPlayer(pm);
        players.add(pm);
        pm.setOp(op);

        return pm;
    }

    public <T extends Event> void assertEventFired(Class<T> clazz) {
        hasFiredEventInstance(clazz).matches(server.getPluginManager());
    }

    public <T extends Event> void assertEventFired(Class<T> clazz, Predicate<T> predicate) {
        hasFiredFilteredEvent(clazz, predicate).matches(server.getPluginManager());
    }

    public void clearEvents() {
        server.getPluginManager().clearEvents();
    }

    private final static int BUFFER = 2048;

    public boolean createZipArchive(File destFile, String srcFolder) {
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)))) {
            addFolder(srcFolder, "", out);
        } catch (Exception e) {
            System.out.println("createZipArchive threw exception: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void addFolder(String srcFolder, String baseFolder, ZipOutputStream out) throws IOException {
        File   subDir       = new File(srcFolder);
        String subdirList[] = subDir.list();
        for (String sd : subdirList) {
            // get a list of files from current directory
            File f = new File(srcFolder + "/" + sd);
            if (f.isDirectory())
                addFolder(f.getAbsolutePath(), baseFolder + "/" + sd, out);
            else {//it is just a file
                addFile(new FileInputStream(f), baseFolder + "/" + sd, out);
            }
        }
    }

    private void addFile(FileInputStream f, String sd, ZipOutputStream out) throws IOException {
        byte            data[] = new byte[BUFFER];
        FileInputStream fi     = f;
        try (BufferedInputStream origin = new BufferedInputStream(fi, BUFFER)) {
            ZipEntry entry = new ZipEntry(sd);
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
                out.flush();
            }
        }
    }
}
