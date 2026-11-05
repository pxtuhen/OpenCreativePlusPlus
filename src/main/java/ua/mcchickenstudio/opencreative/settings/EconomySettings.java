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

package ua.mcchickenstudio.opencreative.settings;

import ua.mcchickenstudio.opencreative.plus.PlusLog;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.economy.DisabledEconomy;
import ua.mcchickenstudio.opencreative.managers.economy.Economy;
import ua.mcchickenstudio.opencreative.managers.economy.VaultEconomy;
import ua.mcchickenstudio.opencreative.utils.hooks.HookUtils;

public final class EconomySettings {

    /**
     * Loads settings of economy from configuration.
     */
    public void load() {
        FileConfiguration config = OpenCreative.getPlugin().getConfig();
        ConfigurationSection section = config.getConfigurationSection("economy");

        String economyType;
        if (section == null) {
            OpenCreative.getPlugin().getConfig().set("economy.type", "auto");
            OpenCreative.getPlugin().saveConfig();
            economyType = "auto";
        } else {
            economyType = section.getString("type", "auto");
        }

        // OpenCreative++: save balances of the previous built-in economy before /oc reload replaces it
        try {
            if (OpenCreative.getEconomy() instanceof ua.mcchickenstudio.opencreative.plus.economy.PlusEconomy previous) {
                previous.shutdown();
            }
        } catch (Exception ignored) {
        }
        OpenCreative.getManagers().register(Economy.class, new DisabledEconomy());
        // OpenCreative++: built-in currency. internal — always own; auto — Vault if installed, else own;
        // vault — only Vault; none — economy off.
        switch (economyType.toLowerCase()) {
            case "none" -> PlusLog.module("Экономика").info("Экономика выключена (economy.type: none).");
            case "vault" -> setupVault();
            case "auto" -> {
                if (HookUtils.isVaultEnabled) setupVault();
                else setupInternal();
            }
            default -> setupInternal();
        }
    }

    private void setupInternal() {
        OpenCreative.getManagers().register(Economy.class, new ua.mcchickenstudio.opencreative.plus.economy.PlusEconomy());
        OpenCreative.getEconomy().start();
    }

    private void setupVault() {
        if (HookUtils.isVaultEnabled) {
            OpenCreative.getPlugin().getLogger().info("Successfully integrated to Vault: Economy actions are working.");
            OpenCreative.getManagers().register(Economy.class, new VaultEconomy());
            OpenCreative.getEconomy().start();
        } else {
            PlusLog.module("Интеграции").debug("Vault не найден: экономика (награды за лайки, покупки) отключена.");
        }
    }

}
