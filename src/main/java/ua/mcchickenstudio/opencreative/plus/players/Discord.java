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

package ua.mcchickenstudio.opencreative.plus.players;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.plus.PlusLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Discord webhook (config.yml → discord): new worlds, contests, market sales.
 */
public final class Discord {

    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private Discord() {
    }

    /**
     * @param event key in discord.events (new-world, contest, market)
     */
    public static void send(@NotNull String event, @NotNull String message) {
        var config = OpenCreative.getPlugin().getConfig();
        String url = config.getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || !config.getBoolean("discord.events." + event, true)) return;
        if (!url.startsWith("https://")) return;
        String text = message.replaceAll("(?i)[&§][0-9a-fk-orx#]", "").replaceAll("<[^>]+>", "");
        JsonObject json = new JsonObject();
        json.addProperty("content", text.length() > 1900 ? text.substring(0, 1900) : text);
        json.addProperty("username", config.getString("discord.name", "OpenCreative++"));
        Bukkit.getScheduler().runTaskAsynchronously(OpenCreative.getPlugin(), () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300) {
                    PlusLog.module("Discord").warn("Вебхук ответил " + response.statusCode() + ": " + response.body());
                }
            } catch (Exception error) {
                PlusLog.module("Discord").warn("Не удалось отправить в Discord: " + error.getMessage());
            }
        });
    }
}
