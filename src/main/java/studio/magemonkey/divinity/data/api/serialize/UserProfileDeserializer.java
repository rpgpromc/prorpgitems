package studio.magemonkey.divinity.data.api.serialize;

import com.google.gson.*;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.data.api.UserEntityNamesMode;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.manager.effects.buffs.SavedBuff;
import studio.magemonkey.divinity.modules.list.classes.api.RPGClass;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class UserProfileDeserializer implements JsonDeserializer<UserProfile> {
    public UserProfile deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws
            JsonParseException {
        JsonObject     j          = json.getAsJsonObject();
        String         id         = j.get("name").getAsString();
        boolean        isDefault  = j.get("isDefault").getAsBoolean();
        Set<SavedBuff> buffDamage = new HashSet<>();
        JsonElement    jBuffsElem = j.get("buffDamage");
        JsonArray      jBuffs     = null;
        if (jBuffsElem != null) {
            jBuffs = jBuffsElem.getAsJsonArray();
            for (JsonElement e : jBuffs)
                buffDamage.add(context.deserialize(e, SavedBuff.class));
        }
        Set<SavedBuff> buffDefense = new HashSet<>();
        jBuffsElem = j.get("buffDefense");
        jBuffs = null;
        if (jBuffsElem != null) {
            jBuffs = jBuffsElem.getAsJsonArray();
            for (JsonElement e : jBuffs)
                buffDefense.add(context.deserialize(e, SavedBuff.class));
        }
        Set<SavedBuff> buffStats = new HashSet<>();
        jBuffsElem = j.get("buffStats");
        jBuffs = null;
        if (jBuffsElem != null) {
            jBuffs = jBuffsElem.getAsJsonArray();
            for (JsonElement e : jBuffs)
                buffStats.add(context.deserialize(e, SavedBuff.class));
        }
        JsonElement eNames       = j.get("namesMode");
        String      namesModeRaw = (eNames != null) ? eNames.getAsString() : null;
        UserEntityNamesMode namesMode =
                (namesModeRaw != null) ? CollectionsUT.getEnum(namesModeRaw, UserEntityNamesMode.class)
                        : UserEntityNamesMode.DEFAULT;
        JsonElement   eHideHelmet = j.get("hideHelmet");
        boolean       hideHelmet  = eHideHelmet != null && eHideHelmet.getAsBoolean();
        UserClassData cData       = null;
        JsonElement   jData       = j.get("cData");
        if (jData != null && Divinity.getInstance().cfg().isModuleEnabled("classes")) {
            JsonObject jClass = jData.getAsJsonObject();
            cData = context.deserialize(jClass, UserClassData.class);
            String   clazzId = cData.getClassId();
            RPGClass clazz   = DivinityAPI.getModuleManager().getClassManager().getClassById(clazzId);
            if (clazz == null) {
                Divinity.getInstance().getLogger().info("Player class '" + clazzId + "' no more exists.");
                cData = null;
            } else {
                cData.setPlayerClass(clazz);
            }
        }
        JsonElement jCooldown = j.get("cCooldown");
        long        cCooldown = 0L;
        if (jCooldown != null)
            cCooldown = jCooldown.getAsLong();
        return new UserProfile(
                id,
                isDefault,

                buffDamage,
                buffDefense,
                buffStats,

                (namesMode == null) ? UserEntityNamesMode.DEFAULT : namesMode,
                hideHelmet,

                cData,
                cCooldown);
    }
}
