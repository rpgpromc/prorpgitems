package com.promcteam.divinity.data;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.promcteam.codex.data.DataTypes;
import com.promcteam.codex.data.IDataHandler;
import org.jetbrains.annotations.NotNull;
import com.promcteam.divinity.QuantumRPG;
import com.promcteam.divinity.data.api.RPGUser;
import com.promcteam.divinity.data.api.UserProfile;
import com.promcteam.divinity.data.api.serialize.SkillDataSerializer;
import com.promcteam.divinity.data.api.serialize.UserProfileSerializer;
import com.promcteam.divinity.modules.list.classes.api.UserSkillData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Function;

public class RPGUserData extends IDataHandler<QuantumRPG, RPGUser> {

    private static RPGUserData instance;

    private final Function<ResultSet, RPGUser> FUNC_USER;

    protected RPGUserData(@NotNull QuantumRPG plugin) throws SQLException {
        super(plugin);

        this.FUNC_USER = (rs) -> {
            try {
                UUID   uuid       = UUID.fromString(rs.getString(COL_USER_UUID));
                String name       = rs.getString(COL_USER_NAME);
                long   lastOnline = rs.getLong(COL_USER_LAST_ONLINE);

                LinkedHashMap<String, UserProfile> profiles =
                        gson.fromJson(rs.getString("profiles"), new TypeToken<LinkedHashMap<String, UserProfile>>() {
                        }.getType());

                return new RPGUser(plugin, uuid, name, lastOnline, profiles);
            } catch (SQLException ex) {
                ex.printStackTrace();
                return null;
            }
        };
    }

    public static RPGUserData getInstance(@NotNull QuantumRPG plugin) throws SQLException {
        if (instance == null) {
            instance = new RPGUserData(plugin);
        }
        return instance;
    }

    @Override
    @NotNull
    protected GsonBuilder registerAdapters(@NotNull GsonBuilder builder) {
        return super.registerAdapters(builder
                .registerTypeAdapter(UserProfile.class, new UserProfileSerializer())
                .registerTypeAdapter(UserSkillData.class, new SkillDataSerializer())
        );
    }

    @Override
    @NotNull
    protected LinkedHashMap<String, String> getColumnsToCreate() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("profiles", DataTypes.STRING.build(this.dataType));
        return map;
    }

    @Override
    @NotNull
    protected LinkedHashMap<String, String> getColumnsToSave(@NotNull RPGUser user) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("profiles", this.gson.toJson(user.getProfileMap()));
        return map;
    }

    @Override
    @NotNull
    protected Function<ResultSet, RPGUser> getFunctionToUser() {
        return this.FUNC_USER;
    }

}
