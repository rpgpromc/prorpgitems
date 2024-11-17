package studio.magemonkey.divinity.modules.list.itemgenerator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.testutil.MockedTest;

import java.io.*;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemGeneratorManagerTest extends MockedTest {

    @BeforeAll
    public void setupAll() throws IOException {
        // Save resources/modules/item_generator/items/common.yml to server directory
        File file = new File(server.getPluginsFolder().getAbsoluteFile() + File.separator +
                "Divinity-" + System.getProperty("DIVINITY_VERSION") + File.separator +
                "modules" + File.separator + "item_generator"
                + File.separator + "items" + File.separator
                + "common.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass()
                .getClassLoader()
                .getResourceAsStream("common.yml"))));
             FileWriter out = new FileWriter(file)) {
            String str;
            while ((str = in.readLine()) != null) {
                out.write(str + "\n");
            }
        }
        Divinity.getInstance().reload();
    }

    @Nested
    class GeneratorItemTest {
        Divinity             divinity;
        ItemGeneratorManager generator;


        @BeforeEach
        void setup() {
            divinity = Divinity.getInstance();
            generator = divinity.getModuleCache().getTierManager();
        }

        @Test
        void getScaleOfLevel_doesNotScaleWith1() {
            ItemGeneratorManager.GeneratorItem item  = generator.getItemById("common");
            double                             scale = item.getScaleOfLevel(1, 5);
            assertEquals(1, scale, 0.001);
        }

        @Test
        void getScaleOfLevel_scalesProperlyUp() {
            ItemGeneratorManager.GeneratorItem item  = generator.getItemById("common");
            double                             scale = item.getScaleOfLevel(1.05, 5);
            assertEquals(1.2, scale, 0.001);
        }

        @Test
        void getScaleOfLevel_scalesProperlyDown() {
            ItemGeneratorManager.GeneratorItem item  = generator.getItemById("common");
            double                             scale = item.getScaleOfLevel(0.95, 5);
            assertEquals(0.8, scale, 0.001);
        }

        @Test
        void getScaleOfLevel_doesNotScaleLevel1() {
            ItemGeneratorManager.GeneratorItem item  = generator.getItemById("common");
            double                             scale = item.getScaleOfLevel(1.05, 1);
            assertEquals(1, scale, 0.001);
        }
    }

}