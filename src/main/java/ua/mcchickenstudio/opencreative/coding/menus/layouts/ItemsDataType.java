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

package ua.mcchickenstudio.opencreative.coding.menus.layouts;


import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class ItemsDataType implements PersistentDataType<byte[], ItemStack[]> {

    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<ItemStack[]> getComplexType() {
        return ItemStack[].class;
    }

    @Override
    public byte @NotNull [] toPrimitive(
            ItemStack @NotNull [] items,
            @NotNull PersistentDataAdapterContext context
    ) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {

            dos.writeInt(items.length);

            for (ItemStack item : items) {
                // Check for null, AIR, or empty items
                if (item == null || item.getType().isAir() || item.isEmpty()) {
                    dos.writeBoolean(false);
                    continue;
                }

                dos.writeBoolean(true);

                byte[] data = item.serializeAsBytes();
                dos.writeInt(data.length);
                dos.write(data);
            }

            dos.flush();
            return bos.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ItemStack[]", e);
        }
    }

    @Override
    public ItemStack @NotNull [] fromPrimitive(
            byte @NotNull [] primitive,
            @NotNull PersistentDataAdapterContext context
    ) {
        if (primitive.length == 0) {
            return new ItemStack[0];
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(primitive);
             DataInputStream dis = new DataInputStream(bis)) {

            int length = dis.readInt();

            if (length < 0 || length > 10000) {
                throw new IOException("Invalid ItemStack array length: " + length);
            }

            ItemStack[] items = new ItemStack[length];

            for (int i = 0; i < length; i++) {
                boolean present = dis.readBoolean();
                if (!present) {
                    continue;
                }

                int dataLength = dis.readInt();
                if (dataLength < 0 || dataLength > primitive.length) {
                    throw new IOException("Invalid ItemStack data length: " + dataLength);
                }

                byte[] data = new byte[dataLength];
                dis.readFully(data);

                items[i] = ItemStack.deserializeBytes(data);
            }

            return items;

        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack[0];
        }
    }
}