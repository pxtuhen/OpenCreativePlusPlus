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

package ua.mcchickenstudio.opencreative.coding.menus.variables;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.menus.ListBrowserMenu;
import ua.mcchickenstudio.opencreative.settings.Sounds;

import java.util.*;

import static ua.mcchickenstudio.opencreative.utils.ItemUtils.*;
import static ua.mcchickenstudio.opencreative.utils.ItemUtils.getCodingParticleTypeKey;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.getLocaleMessage;
import static ua.mcchickenstudio.opencreative.utils.MessageUtils.toKebabCase;

public final class ParticlesMenu extends ListBrowserMenu<Particle> {

    private static final Map<String, Material> particles = new HashMap<>();

    static {
        particles.put("POOF", Material.BONE_MEAL);
        particles.put("EXPLOSION", Material.TNT_MINECART);
        particles.put("EXPLOSION_EMITTER", Material.TNT);
        particles.put("FIREWORK", Material.FIREWORK_ROCKET);
        particles.put("BUBBLE", Material.HEART_OF_THE_SEA);
        particles.put("SPLASH", Material.LAPIS_LAZULI);
        particles.put("FISHING", Material.FISHING_ROD);
        particles.put("UNDERWATER", Material.WET_SPONGE);
        particles.put("CRIT", Material.IRON_SWORD);
        particles.put("ENCHANTED_HIT", Material.NETHERITE_SWORD);
        particles.put("SMOKE", Material.CAMPFIRE);
        particles.put("LARGE_SMOKE", Material.SOUL_CAMPFIRE);
        particles.put("EFFECT", Material.POTION);
        particles.put("INSTANT_EFFECT", Material.WHITE_DYE);
        particles.put("ENTITY_EFFECT", Material.VILLAGER_SPAWN_EGG);
        particles.put("WITCH", Material.WITCH_SPAWN_EGG);
        particles.put("MYCELIUM", Material.MYCELIUM);
        particles.put("NOTE", Material.NOTE_BLOCK);
        particles.put("PORTAL", Material.PURPLE_STAINED_GLASS);
        particles.put("ENCHANT", Material.ENCHANTING_TABLE);
        particles.put("FLAME", Material.CAMPFIRE);
        particles.put("LAVA", Material.LAVA_BUCKET);
        particles.put("CLOUD", Material.QUARTZ);
        particles.put("ITEM_SLIME", Material.SLIME_BALL);
        particles.put("HEART", Material.RED_BED);
        particles.put("ITEM", Material.ITEM_FRAME);
        particles.put("BLOCK", Material.GRASS_BLOCK);
        particles.put("RAIN", Material.WATER_BUCKET);
        particles.put("ELDER_GUARDIAN", Material.ELDER_GUARDIAN_SPAWN_EGG);
        particles.put("DRAGON_BREATH", Material.DRAGON_BREATH);
        particles.put("END_ROD", Material.END_ROD);
        particles.put("SWEEP_ATTACK", Material.BRUSH);
        particles.put("FALLING_DUST", Material.SAND);
        particles.put("SPIT", Material.WHITE_DYE);
        particles.put("SQUID_INK", Material.INK_SAC);
        particles.put("BUBBLE_POP", Material.HEART_OF_THE_SEA);
        particles.put("CURRENT_DOWN", Material.BUCKET);
        particles.put("BUBBLE_COLUMN_UP", Material.WATER_BUCKET);
        particles.put("NAUTILUS", Material.NAUTILUS_SHELL);
        particles.put("DOLPHIN", Material.DOLPHIN_SPAWN_EGG);
        particles.put("SNEEZE", Material.PANDA_SPAWN_EGG);
        particles.put("CAMPFIRE_COSY_SMOKE", Material.CAMPFIRE);
        particles.put("CAMPFIRE_SIGNAL_SMOKE", Material.SOUL_CAMPFIRE);
        particles.put("FLASH", Material.WIND_CHARGE);
        particles.put("FALLING_LAVA", Material.LAVA_BUCKET);
        particles.put("LANDING_LAVA", Material.LAVA_BUCKET);
        particles.put("FALLING_WATER", Material.WATER_BUCKET);
        particles.put("DRIPPING_HONEY", Material.HONEYCOMB_BLOCK);
        particles.put("LANDING_HONEY", Material.HONEY_BLOCK);
        particles.put("FALLING_NECTAR", Material.HONEYCOMB);
        particles.put("SOUL_FIRE_FLAME", Material.SOUL_CAMPFIRE);
        particles.put("ASH", Material.BLACK_DYE);
        particles.put("CRIMSON_SPORE", Material.CRIMSON_PLANKS);
        particles.put("WARPED_SPORE", Material.WARPED_PLANKS);
        particles.put("SOUL", Material.SOUL_SAND);
        particles.put("DRIPPING_OBSIDIAN_TEAR", Material.CRYING_OBSIDIAN);
        particles.put("FALLING_OBSIDIAN_TEAR", Material.OBSIDIAN);
        particles.put("LANDING_OBSIDIAN_TEAR", Material.OBSIDIAN);
        particles.put("REVERSE_PORTAL", Material.PURPLE_SHULKER_BOX);
        particles.put("WHITE_ASH", Material.WHITE_DYE);
        particles.put("DUST_COLOR_TRANSITION", Material.ORANGE_DYE);
        particles.put("FALLING_SPORE_BLOSSOM", Material.SPORE_BLOSSOM);
        particles.put("SPORE_BLOSSOM_AIR", Material.SPORE_BLOSSOM);
        particles.put("SMALL_FLAME", Material.CAMPFIRE);
        particles.put("SNOWFLAKE", Material.SNOW_BLOCK);
        particles.put("DRIPPING_DRIPSTONE_LAVA", Material.LAVA_BUCKET);
        particles.put("FALLING_DRIPSTONE_LAVA", Material.LAVA_BUCKET);
        particles.put("DRIPPING_DRIPSTONE_WATER", Material.WATER_BUCKET);
        particles.put("FALLING_DRIPSTONE_WATER", Material.WATER_BUCKET);
        particles.put("GLOW_SQUID_INK", Material.GLOW_INK_SAC);
        particles.put("GLOW", Material.WHITE_STAINED_GLASS);
        particles.put("WAX_ON", Material.HONEYCOMB);
        particles.put("WAX_OFF", Material.HONEYCOMB);
        particles.put("ELECTRIC_SPARK", Material.QUARTZ);
        particles.put("SCRAPE", Material.TURTLE_HELMET);
        particles.put("SONIC_BOOM", Material.WARDEN_SPAWN_EGG);
        particles.put("SCULK_SOUL", Material.SCULK_CATALYST);
        particles.put("SCULK_CHARGE", Material.SCULK_SENSOR);
        particles.put("SCULK_CHARGE_POP", Material.SCULK);
        particles.put("CHERRY_LEAVES", Material.CHERRY_LEAVES);
        particles.put("EGG_CRACK", Material.TURTLE_EGG);
        particles.put("DUST_PLUME", Material.GUNPOWDER);
        particles.put("WHITE_SMOKE", Material.WHITE_STAINED_GLASS);
        particles.put("GUST", Material.BOW);
        particles.put("GUST_EMITTER_SMALL", Material.ARROW);
        particles.put("GUST_EMITTER_LARGE", Material.SPECTRAL_ARROW);
        particles.put("SMALL_GUST", Material.TIPPED_ARROW);
        particles.put("TRIAL_SPAWNER_DETECTION", Material.TRIAL_SPAWNER);
        particles.put("TRIAL_SPAWNER_DETECTION_OMINOUS", Material.TRIAL_SPAWNER);
        particles.put("VAULT_CONNECTION", Material.VAULT);
        particles.put("DUST_PILLAR", Material.WHITE_CANDLE);
        particles.put("OMINOUS_SPAWNING", Material.SPAWNER);
        particles.put("RAID_OMEN", Material.PILLAGER_SPAWN_EGG);
        particles.put("TRIAL_OMEN", Material.TRIAL_KEY);
        particles.put("BLOCK_MARKER", Material.WHITE_STAINED_GLASS);
        particles.put("DUST", Material.GLOWSTONE_DUST);
        particles.put("ANGRY_VILLAGER", Material.BEETROOT);
        particles.put("HAPPY_VILLAGER", Material.EMERALD);
        particles.put("TOTEM_OF_UNDYING", Material.TOTEM_OF_UNDYING);
        particles.put("DRIPPING_LAVA", Material.LAVA_BUCKET);
        particles.put("DRIPPING_WATER", Material.WATER_BUCKET);
        particles.put("ITEM_COBWEB", Material.COBWEB);
        particles.put("DAMAGE_INDICATOR", Material.NETHERITE_SWORD);
        particles.put("ITEM_SNOWBALL", Material.SNOWBALL);
        particles.put("VIBRATION", Material.SCULK_SENSOR);

        particles.put("COMPOSTER", Material.LIME_DYE);
        particles.put("FALLING_HONEY", Material.HONEYCOMB);
        particles.put("INFESTED", Material.SPIDER_SPAWN_EGG);
        particles.put("COOPER_FIRE_FLAME", Material.SOUL_TORCH);

    }

    public ParticlesMenu(Player player) {
        super(player, getLocaleMessage("menus.developer.particles-list.title"), PlacementLayout.VALUE_CHOOSER);
    }

    private Material getMaterial(Particle type) {
        if (particles.containsKey(type.name())) {
            return particles.get(type.name());
        }
        return Material.NETHER_STAR;
    }

    @Override
    protected ItemStack getElementIcon(Particle particle) {
        ItemStack itemStack = createItem(getMaterial(particle), 1);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.lore(List.of(Component.text(" " + StringUtils.capitalize(particle.name().toLowerCase().replace('_', ' ')) + " ")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.LIGHT_PURPLE), Component.text("")));
            itemStack.setItemMeta(meta);
        }
        setPersistentData(itemStack, getCodingValueKey(), "PARTICLE");
        setPersistentData(itemStack, getCodingParticleTypeKey(), particle.name());
        return itemStack;
    }

    @Override
    protected void fillDecorationItems() {
    }

    @Override
    protected void fillOtherItems() {
    }

    @Override
    protected void onCharmsBarClick(InventoryClickEvent event) {
    }

    @Override
    protected void onElementClick(InventoryClickEvent event) {
        if (isPlayerClicked(event) && isClickedInMenuSlots(event)) {
            if (event.getCurrentItem() == null) return;
            ItemStack item = event.getCurrentItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(getPersistentData(item, getCodingParticleTypeKey()).toLowerCase(Locale.ROOT))
                        .decoration(TextDecoration.ITALIC, false)
                        .color(NamedTextColor.LIGHT_PURPLE));
                meta.lore(null);
                item.setItemMeta(meta);
            }
            item.setType(Material.NETHER_STAR);
            event.getWhoClicked().getInventory().setItemInMainHand(item);
            Sounds.DEV_PARTICLE_SET.play(event.getWhoClicked());
        }
        event.setCancelled(true);
    }

    @Override
    public List<Particle> getElements() {
        return Arrays.stream(Particle.values()).filter(this::doesNotRequiresData).toList();
    }

    @Override
    protected ItemStack getNextPageButton() {
        return createItem(Material.SPECTRAL_ARROW, 1, "menus.developer.particles-list.items.next-page");
    }

    @Override
    protected ItemStack getPreviousPageButton() {
        return createItem(Material.ARROW, 1, "menus.developer.particles-list.items.previous-page");
    }

    @Override
    protected ItemStack getNoElementsButton() {
        return createItem(Material.BARRIER, 1, "menus.developer.particles-list.items.no-elements");
    }

    @Override
    public void onOpen(@NotNull InventoryOpenEvent event) {

    }

    public boolean doesNotRequiresData(Particle particle) {
        Class<?> dataType = particle.getDataType();
        if (dataType == Void.class || dataType == void.class) return true;
        // OpenCreative++: particles with data are spawned with default data (see ParticleData), so they can be chosen too.
        return ua.mcchickenstudio.opencreative.plus.coding.ParticleData.defaultData(particle, new org.bukkit.Location(null, 0, 0, 0)) != null;
    }
}
