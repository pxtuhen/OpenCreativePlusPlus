/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.coding.blocks.actions.playeractions.appearance;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.*;
import jdk.jshell.execution.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.playeractions.PlayerAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.TooLongTextException;
import ua.mcchickenstudio.opencreative.wanders.Wander;

import java.net.URI;
import java.util.List;

public final class SetCapeAction extends PlayerAction {
    public SetCapeAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    public void executePlayer(@NotNull Player player) {
        if (getPlanet().getLimits().cantOpenMenu(player)) {
            return;
        }

        Wander wander = OpenCreative.getWander(player);
        String cape = getArguments().getText("cape", "", this);
        if (cape.length() > 128) {
            throw new TooLongTextException(128);
        }
        String capeUrl;
        if (cape.isEmpty()) {
            capeUrl =  "";
        } else {
            capeUrl = "http://textures.minecraft.net/texture/"+cape;
        }
        PlayerProfile profile = player.getPlayerProfile();
        // this is a mess
        String textures = new String(java.util.Base64.getDecoder().decode(profile.getProperties().stream().findFirst().get().getValue()));
        Bukkit.getLogger().info(textures);
        JsonElement texturesJson = JsonParser.parseString(textures);
        JsonObject object = texturesJson.getAsJsonObject();
        object.getAsJsonObject("textures").getAsJsonObject("CAPE").addProperty("url",capeUrl);
        Gson gson = new Gson();
        textures = gson.toJson(object);

        textures = java.util.Base64.getEncoder().encodeToString(textures.getBytes());
        profile.setProperty(new ProfileProperty("textures",textures));
        try {
            wander.setTexturesWereChanged(true);
            player.setPlayerProfile(profile);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.PLAYER_SET_CAPE;
    }
}
