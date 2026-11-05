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

package ua.mcchickenstudio.opencreative.managers.updater;

import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.managers.Manager;
import ua.mcchickenstudio.opencreative.managers.Startable;

import java.util.concurrent.CompletableFuture;

/**
 * <h1>Updater</h1>
 * This interface represents a update manager,
 * that tells server to update or not.
 */
public interface Updater extends Manager, Startable {

    /**
     * Sends web request and checks if new updates
     * of OpenCreative+ are available. It's called once
     * on plugin enable and on command usage.
     */
    @NotNull CompletableFuture<String> checkUpdates();

    /**
     * Returns updates availability for OpenCreative+.
     * It gets saved value from {@link #checkUpdates()}.
     *
     * @return true - if updates are available, false - if not.
     */
    boolean canBeUpdated();

}
