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

package ua.mcchickenstudio.opencreative.planets;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.utils.ItemUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static ua.mcchickenstudio.opencreative.utils.ErrorUtils.sendCriticalErrorMessage;
import static ua.mcchickenstudio.opencreative.utils.FileUtils.getPlayerDataJson;

/**
 * <h1>PlanetPlayer</h1>
 * This class represents a Player in planet, he has saved purchases,
 * inventory, own ender chest and other required parameters.
 * Saved data stores in planet's world as /playersData/UUID.json.
 */
public class PlanetPlayer {

    private final Planet currentPlanet;
    private final Player player;

    private Double worldSize;

    private final Set<String> purchases = new HashSet<>();
    private final Map<Integer, ItemStack[]> savedInventory = new HashMap<>();
    private final ItemStack[] savedEnderChest = new ItemStack[54];

    public PlanetPlayer(@NotNull Planet currentPlanet, @NotNull Player player) {
        this.currentPlanet = currentPlanet;
        this.player = player;
    }

    public void setWorldSize(@Nullable Double worldSize) {
        this.worldSize = worldSize;
    }

    public @Nullable Double getWorldSize() {
        return worldSize;
    }

    /**
     * Returns planet, where player is registered.
     *
     * @return associated planet.
     */
    @SuppressWarnings("unused")
    public @NotNull Planet getCurrentPlanet() {
        return currentPlanet;
    }

    /**
     * Returns Bukkit's player.
     *
     * @return World player as Bukkit's player.
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    public ItemStack[] getSavedInventory(int number) {
        ItemStack[] items = savedInventory.get(number);
        if (items == null) {
            return new ItemStack[43];
        }
        return items;
    }

    public ItemStack[] getSavedEnderChest() {
        return savedEnderChest;
    }

    /**
     * Saves items array as inventory.
     * Used in player action "Save Inventory".
     *
     * @param items Array of ItemStacks to save.
     */
    public void saveInventory(ItemStack[] items, int number) {
        ItemStack[] savedInventory = getSavedInventory(number);
        int slot = 0;
        int badItems = 0;
        for (ItemStack item : items) {
            if (savedInventory.length == slot) break;
            if (item != null) {
                badItems += ItemUtils.getInsideBadItemsAmount(item, 2);
                if (badItems >= 3) item = new ItemStack(Material.AIR);
            }
            savedInventory[slot] = item;
            slot++;
        }
        this.savedInventory.put(number, savedInventory);
    }

    /**
     * Saves items array as items in ender chest.
     * Used for saving player's own ender chest.
     *
     * @param items Array of ItemStacks to save.
     */
    public void saveEnderChest(ItemStack[] items) {
        Arrays.fill(savedEnderChest, null);
        int slot = 0;
        for (ItemStack item : items) {
            if (savedEnderChest.length == slot) break;
            savedEnderChest[slot] = item;
            slot++;
        }
    }

    /**
     * Loads saved player data from JSON file, that
     * stored in planet's folder as /playerData/UUID.json.
     *
     * @return true - if successfully loaded, false - if failed to load.
     */
    @SuppressWarnings("unchecked")
    public @NotNull CompletableFuture<Void> load() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            File playerDataJson = getPlayerDataJson(currentPlanet, player);
            if (playerDataJson == null) {
                future.complete(null);
                return;
            }
            if (playerDataJson.length() == 0) {
                future.complete(null);
                return;
            }
            JSONParser parser = new JSONParser();
            try (FileReader fileReader = new FileReader(playerDataJson)) {
                JSONObject playerObject = (JSONObject) parser.parse(fileReader);
                Object purchases = playerObject.getOrDefault("purchases", new JSONArray());
                if (purchases instanceof JSONArray array) {
                    this.purchases.addAll(array);
                }
                Object savedInventories = playerObject.get("saved-inventories");
                if (savedInventories instanceof JSONObject map) {
                    for (Object key : map.keySet()) {
                        Object value = map.get(key);
                        if (value instanceof JSONArray array) {
                            int id;
                            try {
                                if (key instanceof String string) {
                                    id = Integer.parseInt(string);
                                } else {
                                    continue;
                                }
                            } catch (Exception error) {
                                continue;
                            }
                            List<ItemStack> items = new ArrayList<>();
                            for (Object object : array) {
                                items.add(ItemUtils.loadItemFromByteArray((String) object));
                            }
                            saveInventory(items.toArray(new ItemStack[]{}), id);
                        }
                    }
                }
                Object savedInventory = playerObject.get("saved-inventory");
                if (savedInventory instanceof JSONArray array) {
                    List<ItemStack> items = new ArrayList<>();
                    for (Object object : array) {
                        items.add(ItemUtils.loadItemFromByteArray((String) object));
                    }
                    saveInventory(items.toArray(new ItemStack[]{}), 1);
                }
                Object savedEnderChest = playerObject.getOrDefault("saved-ender-chest", new JSONArray());
                if (savedEnderChest instanceof JSONArray array) {
                    List<ItemStack> items = new ArrayList<>();
                    for (Object object : array) {
                        items.add(ItemUtils.loadItemFromByteArray((String) object));
                    }
                    saveEnderChest(items.toArray(new ItemStack[]{}));
                }
                future.complete(null);
            } catch (Exception error) {
                sendCriticalErrorMessage("Couldn't read player data " + player.getName()
                        + " " + currentPlanet.getWorldName(), error);
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    /**
     * Saves some required player data into JSON file
     * in planet's folder as /playerData/UUID.json.
     */
    public void save() {
        if (OpenCreative.getPlugin().isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), this::saveToJson);
        } else {
            saveToJson();
        }
    }

    /**
     * Saves player data to JSON.
     */
    @SuppressWarnings("unchecked")
    private void saveToJson() {
        File playerDataJson = getPlayerDataJson(currentPlanet, player);
        if (playerDataJson == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(playerDataJson)) {
            JSONObject playerObject = new JSONObject();

            JSONArray purchasesJson = new JSONArray();
            purchasesJson.addAll(purchases);
            playerObject.put("purchases", purchasesJson);

            JSONObject savedInventoryJson = serializeInventories(savedInventory);
            playerObject.put("saved-inventories", savedInventoryJson);
            JSONArray enderChestJson = serializeItems(savedEnderChest);
            playerObject.put("saved-ender-chest", enderChestJson);

            writer.write(playerObject.toString());
        } catch (Exception e) {
            sendCriticalErrorMessage("Couldn't save player data "
                    + player.getName() + " " + currentPlanet.getWorldName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull JSONArray serializeItems(ItemStack[] items) {
        JSONArray json = new JSONArray();
        if (items == null) {
            return json;
        }
        for (ItemStack item : items) {
            json.add(ItemUtils.saveItemAsByteArray(item));
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    private @NotNull JSONObject serializeInventories(Map<Integer, ItemStack[]> inventories) {
        JSONObject json = new JSONObject();
        for (Map.Entry<Integer, ItemStack[]> entry : inventories.entrySet()) {
            int id = entry.getKey();
            ItemStack[] items = entry.getValue();
            json.put(String.valueOf(id), serializeItems(items));
        }
        return json;
    }

    /**
     * Returns set of saved purchases IDs.
     *
     * @return set of saved purchases IDs.
     */
    public @NotNull Set<String> getPurchases() {
        return purchases;
    }

    /**
     * Adds purchase ID into set of saved player's purchases.
     *
     * @param id ID of purchase.
     */
    public void addPurchase(String id) {
        purchases.add(id);
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }
}
