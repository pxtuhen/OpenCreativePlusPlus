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

package ua.mcchickenstudio.opencreative.managers.disguises;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.MannequinWatcher;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.utils.hooks.HookUtils;
import java.util.Optional;
import java.util.UUID;

public final class LibsDisguises implements DisguiseManager {

    private Boolean mannequinExists;

    @Override
    public void disguiseAsPlayer(@NotNull Entity entity, @NotNull String skin, @NotNull String nickname) {
        try {
            if (checkMannequinsSupport()) {
                // above 1.21.10
                MobDisguise disguise = new MobDisguise(DisguiseType.MANNEQUIN);
                ((MannequinWatcher)disguise.getWatcher()).setSkin(skin);
                ((MannequinWatcher)disguise.getWatcher()).setDescription(Optional.empty());
                disguise.getWatcher().setCustomName(nickname);
                disguise.setEntity(entity);
                disguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
                disguise.startDisguise();
            } else {
                // below 1.21.10
                PlayerDisguise disguise = PlayerDisguise.class
                        .getDeclaredConstructor(String.class, String.class)
                        .newInstance(nickname, skin);
                disguise.setEntity(entity);
                disguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
                disguise.startDisguise();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void setDisguiseDisplayName(@NotNull Entity entity, @NotNull String displayName) {
        Disguise disguise = DisguiseAPI.getDisguise(entity);
        if (disguise == null) return;
        if (disguise instanceof PlayerDisguise mobDisguise) {
            mobDisguise.setName(displayName);
        }
    }

    @Override
    public void disguiseAsEntity(@NotNull Entity entity, @NotNull EntityType type) {
        MobDisguise mobDisguise = new MobDisguise(DisguiseType.getType(type));
        mobDisguise.setEntity(entity);
        mobDisguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
        mobDisguise.startDisguise();
    }

    @Override
    public void disguiseAsBlock(@NotNull Entity entity, @NotNull Material material) {
        MiscDisguise disguise = new MiscDisguise(DisguiseType.FALLING_BLOCK, material);
        disguise.setEntity(entity);
        disguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
        disguise.startDisguise();
    }
    @Override
    public void  disguiseAsExistingEntity(@NotNull Entity entity, @NotNull Entity entity2)
    {
        if (entity2 instanceof Player player)
        {
            // for if mannequins are preferred
            disguiseAsPlayer(entity, player.getName(), player.getName());
            return;
        }
       Disguise disguise = DisguiseAPI.constructDisguise(entity2);
       disguise.setEntity(entity);
        disguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
        disguise.startDisguise();
    }

    @Override
    public void clearDisguises(@NotNull Entity entity) {
        Disguise disguise = DisguiseAPI.getDisguise(entity);
        if (disguise == null) return;
        disguise.stopDisguise();
    }

    /**
     * Checks whether mannequins are supported on this server version.
     *
     * @return true - supported, false - not.
     */
    private boolean checkMannequinsSupport() {
        if (mannequinExists == null) {
            try {
                Class.forName("org.bukkit.entity.Mannequin");
                mannequinExists = true;
            } catch (Exception ignored) {
                mannequinExists = false;
            }
        }
        return mannequinExists;
    }

    @Override
    public void start() {}

    @Override
    public void shutdown() {}

    @Override
    public boolean isWorking() {
        return HookUtils.isLibsDisguisesEnabled;
    }

    @Override
    public @NotNull String getName() {
        return "LibsDisguises";
    }
}
