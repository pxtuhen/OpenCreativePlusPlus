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

package ua.mcchickenstudio.opencreative.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.managers.ignore.EssentialsIgnore;
import ua.mcchickenstudio.opencreative.managers.ignore.IgnoreManager;

public final class ServerListener implements Listener {

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        /*
         * Registers ignore manager for Essentials plugin
         * after its loading. Essentials plugin is not specified in
         * "load before" list, because it overrides our commands.
         */
        if (event.getPlugin().getName().equals("Essentials")) {
            OpenCreative.getManagers().register(IgnoreManager.class, new EssentialsIgnore());
        }
    }

}
