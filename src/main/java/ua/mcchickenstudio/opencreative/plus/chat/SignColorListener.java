/*
 * OpenCreative++, fork of OpenCreative+ (C) 2022-2026, McChicken Studio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.plus.chat;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;

import java.util.ArrayList;
import java.util.List;

/**
 * Colour codes on signs (both sides).
 * <p>
 * Text with codes, as player typed it, is kept in the sign's data, so when the sign is
 * opened again, the player sees {@code &aHello} instead of already coloured text.
 */
public final class SignColorListener implements Listener {

    private static final String SEPARATOR = "\n";

    private static @NotNull NamespacedKey rawKey(@NotNull Side side) {
        return new NamespacedKey(OpenCreative.getPlugin(), "sign_raw_" + side.name().toLowerCase());
    }

    // After OpenCreative+ filter (NORMAL), so filtered signs are already cancelled.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        ColorFormatter.Styles styles = ColorFormatter.styles(event.getPlayer(), ColorFormatter.Place.SIGN, null);
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        List<String> raw = new ArrayList<>();
        boolean hasCodes = false;
        for (int i = 0; i < event.lines().size(); i++) {
            Component line = event.line(i);
            String text = line == null ? "" : plain.serialize(line);
            raw.add(text);
            if (text.indexOf('&') >= 0 || text.indexOf('§') >= 0) {
                hasCodes = true;
                if (styles.any()) event.line(i, ColorFormatter.format(styles, text));
            }
        }
        Block block = event.getBlock();
        Side side = event.getSide();
        boolean keepRaw = hasCodes && styles.any();
        // Sign is updated with new lines after the event, so data is written on the next tick.
        Bukkit.getScheduler().runTask(OpenCreative.getPlugin(), () -> {
            if (!(block.getState() instanceof Sign sign)) return;
            if (keepRaw) {
                sign.getPersistentDataContainer().set(rawKey(side), PersistentDataType.STRING, String.join(SEPARATOR, raw));
            } else if (sign.getPersistentDataContainer().has(rawKey(side))) {
                sign.getPersistentDataContainer().remove(rawKey(side));
            } else {
                return;
            }
            sign.update(true, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignOpen(PlayerOpenSignEvent event) {
        if (!OpenCreative.getPlugin().getConfig().getBoolean("chat.signs.show-codes-on-edit", true)) return;
        if (event.getCause() == PlayerOpenSignEvent.Cause.PLACE) return;
        Sign sign = event.getSign();
        Side side = event.getSide();
        String stored = sign.getPersistentDataContainer().get(rawKey(side), PersistentDataType.STRING);
        if (stored == null) return;
        String[] lines = stored.split(SEPARATOR, -1);
        for (int i = 0; i < 4; i++) {
            sign.getSide(side).line(i, Component.text(i < lines.length ? lines[i] : ""));
        }
        sign.update(true, false);
    }
}
