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

package ua.mcchickenstudio.opencreative.plus.web;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ua.mcchickenstudio.opencreative.OpenCreative;
import ua.mcchickenstudio.opencreative.planets.Planet;
import ua.mcchickenstudio.opencreative.plus.PlusLog;
import ua.mcchickenstudio.opencreative.plus.contest.Contests;
import ua.mcchickenstudio.opencreative.plus.rating.Ratings;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Public website with the list of worlds (config.yml → web.site): top by rating, online now, new;
 * page of every world; current contest; JSON API /api/worlds and /api/contest.
 * <p>
 * Data is copied on the main thread every 30 seconds; HTTP handlers only read that snapshot.
 */
public final class WebSite {

    private static final PlusLog log = PlusLog.module("Сайт");
    private static final Gson gson = new Gson();
    private static HttpServer server;
    private static BukkitTask snapshotTask;
    private static volatile List<WorldCard> worlds = List.of();
    private static volatile Map<String, Object> contest = Map.of();

    /**
     * World data for the site.
     */
    public record WorldCard(int id, String name, String description, String owner, double rating, int votes,
                            int online, String category, String icon, String customId, long created, int visitors) {
    }

    private WebSite() {
    }

    private static @NotNull FileConfiguration config() {
        return OpenCreative.getPlugin().getConfig();
    }

    public static void start() {
        stop();
        if (!config().getBoolean("web.site.enabled", true)) return;
        int port = config().getInt("web.site.port", 22913);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", WebSite::handle);
            server.setExecutor(Executors.newFixedThreadPool(2));
            server.start();
            snapshot();
            // worlds are added asynchronously after startup, so the first real snapshot is taken a bit later
            snapshotTask = Bukkit.getScheduler().runTaskTimer(OpenCreative.getPlugin(), WebSite::snapshot, 100L, 600L);
            log.info("Сайт запущен: http://localhost:" + port + "/");
        } catch (Exception error) {
            server = null;
            log.error("Сайт не запущен (порт " + port + " занят? web.site.port в config.yml)", error);
        }
    }

    public static void stop() {
        if (snapshotTask != null) snapshotTask.cancel();
        snapshotTask = null;
        if (server != null) server.stop(0);
        server = null;
    }

    private static @NotNull String plain(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text)).replace("\\n", " ");
    }

    private static void snapshot() {
        List<WorldCard> cards = new ArrayList<>();
        for (Planet planet : OpenCreative.getPlanetsManager().getPlanets()) {
            if (planet.getSharing() != Planet.Sharing.PUBLIC) continue;
            cards.add(new WorldCard(planet.getId(), plain(planet.getInformation().getDisplayName()),
                    plain(planet.getInformation().getDescription()), planet.getOwnerName(), Ratings.average(planet),
                    Ratings.votes(planet), planet.getInformation().getAsyncOnline(),
                    planet.getInformation().getCategory().name().toLowerCase(),
                    planet.getInformation().getIcon().getType().getKey().getKey(), planet.getInformation().getCustomID(),
                    planet.getCreationTime(), planet.getInformation().getUniques()));
        }
        worlds = List.copyOf(cards);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("phase", Contests.phase().name().toLowerCase());
        info.put("name", Contests.name());
        info.put("theme", Contests.theme());
        info.put("ends", Contests.phaseEnds());
        info.put("participants", Contests.entries().size());
        info.put("results", Contests.results().stream().map(WebSite::plain).toList());
        contest = info;
    }

    // ------------------------------------------------------------------ http

    private static void handle(@NotNull HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/worlds")) {
                send(exchange, 200, "application/json", gson.toJson(worlds));
            } else if (path.equals("/api/contest")) {
                send(exchange, 200, "application/json", gson.toJson(contest));
            } else if (path.startsWith("/world/")) {
                String id = path.substring(7);
                Optional<WorldCard> card = worlds.stream().filter(world -> String.valueOf(world.id()).equals(id)).findFirst();
                if (card.isPresent()) send(exchange, 200, "text/html", worldPage(card.get()));
                else send(exchange, 404, "text/html", page("404", "<p class=\"muted\">Мир не найден.</p>"));
            } else if (path.equals("/") || path.equals("/index.html")) {
                send(exchange, 200, "text/html", indexPage());
            } else {
                send(exchange, 404, "text/html", page("404", "<p class=\"muted\">Страница не найдена.</p>"));
            }
        } catch (Exception error) {
            log.debug("Ошибка запроса: " + error.getMessage());
        } finally {
            exchange.close();
        }
    }

    private static void send(@NotNull HttpExchange exchange, int code, @NotNull String type, @NotNull String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static @NotNull String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ------------------------------------------------------------------ pages

    private static @NotNull String indexPage() {
        List<WorldCard> list = new ArrayList<>(worlds);
        StringBuilder body = new StringBuilder();
        String phase = String.valueOf(contest.getOrDefault("phase", "none"));
        if (phase.equals("building") || phase.equals("voting")) {
            body.append("<section class=\"contest\"><h2>🏆 Конкурс: ").append(escape(String.valueOf(contest.get("name"))))
                    .append("</h2><p>Тема: <b>").append(escape(String.valueOf(contest.get("theme")))).append("</b> · ")
                    .append(phase.equals("building") ? "идёт стройка" : "идёт голосование")
                    .append(" · участников: ").append(contest.get("participants")).append("</p><p class=\"muted\">Участвовать: <code>/contest</code></p></section>");
        }
        List<WorldCard> online = list.stream().filter(world -> world.online() > 0)
                .sorted(Comparator.comparingInt(WorldCard::online).reversed()).limit(12).toList();
        if (!online.isEmpty()) body.append(section("Сейчас играют", online));
        body.append(section("Лучшие миры", list.stream().filter(world -> world.votes() > 0)
                .sorted(Comparator.comparingDouble((WorldCard world) -> (3 * 3.0 + world.rating() * world.votes()) / (3 + world.votes())).reversed())
                .limit(24).toList()));
        body.append(section("Новые миры", list.stream().sorted(Comparator.comparingLong(WorldCard::created).reversed()).limit(12).toList()));
        return page(config().getString("web.site.title", "OpenCreative++"), body.toString());
    }

    private static @NotNull String section(@NotNull String title, @NotNull List<WorldCard> list) {
        StringBuilder html = new StringBuilder("<section><h2>").append(escape(title)).append("</h2>");
        if (list.isEmpty()) return html.append("<p class=\"muted\">Пока пусто.</p></section>").toString();
        html.append("<div class=\"grid\">");
        for (WorldCard world : list) {
            html.append("<a class=\"card\" href=\"/world/").append(world.id()).append("\">")
                    .append("<div class=\"name\">").append(escape(world.name())).append("</div>")
                    .append("<div class=\"muted\">").append(escape(world.owner())).append(" · ").append(escape(world.category())).append("</div>")
                    .append("<div class=\"stats\"><span>★ ").append(world.votes() == 0 ? "—" : String.format(Locale.ROOT, "%.1f", world.rating()))
                    .append(" (").append(world.votes()).append(")</span><span>👤 ").append(world.online()).append("</span></div>")
                    .append("<code>/join ").append(escape(world.customId())).append("</code></a>");
        }
        return html.append("</div></section>").toString();
    }

    private static @NotNull String worldPage(@NotNull WorldCard world) {
        String body = "<a class=\"muted\" href=\"/\">← Все миры</a><h1>" + escape(world.name()) + "</h1>"
                + "<p>" + escape(world.description()) + "</p>"
                + "<div class=\"facts\"><div><b>Автор</b>" + escape(world.owner()) + "</div>"
                + "<div><b>Оценка</b>★ " + (world.votes() == 0 ? "—" : String.format(Locale.ROOT, "%.1f", world.rating())) + " (" + world.votes() + ")</div>"
                + "<div><b>Сейчас играют</b>" + world.online() + "</div>"
                + "<div><b>Посетителей</b>" + world.visitors() + "</div>"
                + "<div><b>Категория</b>" + escape(world.category()) + "</div>"
                + "<div><b>Создан</b>" + new SimpleDateFormat("dd.MM.yyyy").format(new Date(world.created())) + "</div></div>"
                + "<p>Зайти на сервер и написать: <code>/join " + escape(world.customId()) + "</code></p>";
        return page(world.name(), body);
    }

    private static @NotNull String page(@NotNull String title, @NotNull String body) {
        String server = escape(config().getString("web.site.title", "OpenCreative++"));
        String address = escape(config().getString("web.site.server-address", ""));
        return "<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escape(title) + "</title><style>"
                + ":root{--bg:#f6f7fb;--card:#fff;--text:#1b1d24;--muted:#6b7080;--accent:#5b6cff;--line:#e3e5ee}"
                + "@media(prefers-color-scheme:dark){:root{--bg:#121318;--card:#1c1e26;--text:#eceef4;--muted:#9aa0b2;--accent:#8c98ff;--line:#2a2d38}}"
                + "*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:var(--bg);color:var(--text)}"
                + "header{padding:24px 16px;border-bottom:1px solid var(--line)}header div{max-width:1100px;margin:0 auto;display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap}"
                + "main{max-width:1100px;margin:0 auto;padding:16px}h1{margin:16px 0 8px}h2{margin:24px 0 12px;font-size:20px}"
                + ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:12px}"
                + ".card{display:block;padding:14px;border:1px solid var(--line);border-radius:12px;background:var(--card);color:inherit;text-decoration:none}"
                + ".card:hover{border-color:var(--accent)}.name{font-weight:600;margin-bottom:4px;overflow-wrap:anywhere}"
                + ".stats{display:flex;gap:12px;margin:8px 0}.muted{color:var(--muted)}a.muted{text-decoration:none}"
                + "code{background:var(--line);padding:2px 6px;border-radius:6px;font-size:13px}"
                + ".contest{padding:16px;border-radius:12px;background:var(--card);border:1px solid var(--accent)}"
                + ".facts{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:10px;margin:16px 0}"
                + ".facts div{padding:10px;border:1px solid var(--line);border-radius:10px;background:var(--card)}.facts b{display:block;font-size:12px;color:var(--muted);font-weight:500}"
                + "footer{max-width:1100px;margin:24px auto;padding:16px;color:var(--muted);font-size:13px}"
                + "</style></head><body><header><div><b style=\"font-size:20px\">" + server + "</b>"
                + (address.isEmpty() ? "" : "<span>Сервер: <code>" + address + "</code></span>") + "</div></header>"
                + "<main>" + body + "</main><footer>OpenCreative++ · основан на OpenCreative+ (GNU AGPL v3)</footer></body></html>";
    }
}
