package studio.magemonkey.divinity.utils;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import studio.magemonkey.codex.Codex;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.items.CodexItemManager;
import studio.magemonkey.codex.modules.ModuleManager;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.arrows.ArrowManager;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DivinityProviderTest {
    private Divinity                  divinity;
    private MockedStatic<Divinity>    divinityStatic;
    private MockedStatic<CodexEngine> codexEngineStatic;
    private ModuleManager             moduleManager;

    private ArrowManager         arrowModule;
    private ItemGeneratorManager itemGenModule;

    private CodexItemManager itemManager;
    private DivinityProvider provider = new DivinityProvider();

    @BeforeEach
    void setUp() {
        divinity = mock(Divinity.class);
        when(divinity.getName()).thenReturn("Divinity");

        divinityStatic = mockStatic(Divinity.class);
        divinityStatic.when(Divinity::getInstance).thenReturn(divinity);

        CodexEngine codex = mock(CodexEngine.class);
        when(codex.getLogger()).thenReturn(Logger.getLogger("CodexEngine"));
        codexEngineStatic = mockStatic(CodexEngine.class);
        codexEngineStatic.when(CodexEngine::get).thenReturn(codex);
        Codex.setPlugin(codex);
        itemManager = new CodexItemManager(codex);
        itemManager.init();
        itemManager.registerProvider("DIVINITY", provider);
        when(codex.getItemManager()).thenReturn(itemManager);

        moduleManager = mock(ModuleManager.class);

        arrowModule = new ArrowManager(divinity);
        when(moduleManager.getModule("arrows")).thenReturn(arrowModule);

        itemGenModule = spy(new ItemGeneratorManager(divinity));
        when(moduleManager.getModule("item_generator")).thenReturn(itemGenModule);

        //noinspection unchecked
        when(divinity.getModuleManager()).thenReturn(moduleManager);
    }

    @AfterEach
    void afterEach() {
        divinityStatic.close();
        codexEngineStatic.close();
    }

    @Test
    void getItem_usesLevel() {
        ItemGeneratorManager.GeneratorItem generatorItem = mock(ItemGeneratorManager.GeneratorItem.class);
        doReturn(generatorItem).when(itemGenModule).getItemById("foobar");

        DivinityProvider.DivinityItemType item = provider.getItem("DIVINITY_item_generator:foobar~level:5");

        assertNotNull(item);
        verify(itemGenModule).getItemById("foobar");
        assertEquals(5, item.getLevel());
        assertNull(item.getMaterial());
        assertEquals(generatorItem, item.getModuleItem());
        assertInstanceOf(DivinityProvider.DivinityItemType.class, item);
    }

    @Test
    void getItem_usesMaterial() {
        ItemGeneratorManager.GeneratorItem generatorItem = mock(ItemGeneratorManager.GeneratorItem.class);
        doReturn(generatorItem).when(itemGenModule).getItemById("foobar");

        DivinityProvider.DivinityItemType item =
                provider.getItem("DIVINITY_item_generator:foobar~material:VANILLA_DIAMOND");

        assertNotNull(item);
        verify(itemGenModule).getItemById("foobar");
        assertEquals(-1, item.getLevel());
        assertEquals(Material.DIAMOND.name().toLowerCase(), item.getMaterial().getID());
        assertEquals("VANILLA", item.getMaterial().getNamespace());
        assertEquals(generatorItem, item.getModuleItem());
        assertInstanceOf(DivinityProvider.DivinityItemType.class, item);
    }
}