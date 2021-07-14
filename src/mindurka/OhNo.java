package mindurka;

import arc.Events;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.core.NetClient;
import mindustry.mod.*;
import mindustry.net.*;
import mindustry.type.UnitType;
import org.json.JSONObject;

import java.io.IOException;
import java.awt.*;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static mindustry.Vars.*;

@SuppressWarnings("unused")
public class OhNo extends Plugin {

    private static final EffectData moveEffect = new EffectData(0, 0, 30, 0, "#4169e1ff", "freezing");
    private static final EffectData joinEffect = new EffectData(0, 0, 30, 0, "#4169e1ff", "greenBomb");
    private static final EffectData leaveEffect = new EffectData(0, 0, 30, 0, "#4169e1ff", "greenLaserCharge");

    private final HashSet<String> votes = new HashSet<>();
    private ConfigurationManager config;
    private JSONObject jsonData;
    private boolean anarchy;
    private double ratio;
    private boolean vnw;
    private boolean spawn;
    private boolean tp;
    private boolean effects;
    private boolean prefixes;

    private final String hook = "https://discord.com/api/webhooks/860117788323348480/1N154rKyxvRPafqwEiOKGLHJQ7O4RkrgKzTSTHspGMEjDkhdIqNJcaQBy7rMRKgX8dZU";
    public final Seq<RainbowPlayerEntry> rainbow = new Seq<>();

    public OhNo() {
        try {
            this.config = new ConfigurationManager();
            this.jsonData = this.config.getJsonData();

            this.anarchy = this.jsonData.getBoolean("anarchy");
            this.ratio = this.jsonData.getDouble("ratio");
            this.vnw = this.jsonData.getBoolean("vnw");
            this.spawn = this.jsonData.getBoolean("spawn");
            this.tp = this.jsonData.getBoolean("tp");
            this.effects = this.jsonData.getBoolean("effects");
            this.prefixes = this.jsonData.getBoolean("prefixes");

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {

        Administration.Config.showConnectMessages.set(false);

        Events.on(PlayerJoin.class, playerJoin -> {
            Call.sendMessage("[lime]+[] " + playerJoin.player.name() + " [accent]зашёл на сервер");
            Log.info(playerJoin.player.name + " зашёл на сервер, IP: " + playerJoin.player.ip() + ", ID: " + playerJoin.player.uuid());
            int players = Groups.player.size();
            if (anarchy) playerJoin.player.admin = true;
            if (players == 1) {
                Vars.state.serverPaused = false;
                Log.info("Сервер снова онлайн!");
            }
            setPrefix(playerJoin.player);
            if (effects) {
                try { joinEffect.spawn(playerJoin.player.x, playerJoin.player.y); }
                catch (NullPointerException e) {}
            }
            Call.infoPopup(playerJoin.player.con(), "[accent]Ты играешь на карте[lime]" + "\n" + Vars.state.map.name() + "\n" + "[accent]Автор карты:" + "\n" + Vars.state.map.author(), 15f, 20, 50, 20, 450, 0);
        });

        Events.on(PlayerLeave.class, playerLeave -> {
            Player player = playerLeave.player;
            Groups.player.update();
            int players = Groups.player.size();
            Call.sendMessage("[scarlet]-[] " + player.name() + " [accent]вышел с сервера");
            Log.info(player.name + " вышел с сервера, IP: " + player.ip() + ", ID: " + player.uuid());
            if (effects) {
                try { leaveEffect.spawn(playerLeave.player.x, playerLeave.player.y); }
                catch (NullPointerException e) {}
            }
            if (players == 1) {
                Vars.state.serverPaused = true;
                Log.info("Сервер поставлен на паузу!");
            }
            rainbow.remove(p -> p.player.uuid().equals(playerLeave.player.uuid()));
            if (!votes.contains(player.uuid())) return;
            votes.remove(player.uuid());
            int currentVotes = votes.size();
            int requiredVotes = (int) Math.ceil(ratio * players);
            Call.sendMessage("[cyan]VNW[]: [accent]" + player.name + "[] покинул сервер, [green]" + currentVotes + "[] голос(ов), [green]" + requiredVotes + "[] необходимо.");
        });

        Events.on(GameOverEvent.class, e -> this.votes.clear());

        if (effects) {
            Events.run(Trigger.update, () -> Groups.player.each(p -> p.unit().moving(), p -> moveEffect.spawn(p.x, p.y)));
        }

        Timer.schedule(() -> rainbow.each(r -> Groups.player.contains(p -> p == r.player), r -> {
            int hue = r.hue;
            if(hue < 360){
                hue++;
            }else{
                hue = 0;
            }

            String hex = "[#" + Integer.toHexString(Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2) + "]";
            r.player.name = hex + r.stripedName;
            r.hue = hue;
        }), 0f, 0.05f);
    }

    public static class RainbowPlayerEntry {
        public Player player;
        public int hue;
        public String stripedName;
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("ut","unit type", (args, player) -> {
            try { info("твой офигенный юнит - [sky]" + player.unit().type().name + "[].", player); }
            catch (NullPointerException e) { err("у тебя нет юнита, клоун...", player); }
        });

        handler.<Player>register("admins", "Список админов", (arg, player) -> {
            Seq<Administration.PlayerInfo> admins = Vars.netServer.admins.getAdmins();
            if (anarchy) {
                err("интересно, как ты додумался смотреть админов там, где админы - все. Гений мысли.", player);
                return;
            }
            if (admins.size == 0) {
                err("[scarlet]админов нет.", player);
                return;
            }
            info("админы:", player);
            admins.each((admin) -> player.sendMessage(admin.lastName));
        });

        handler.<Player>register("tp", "<X> <Y>", "Телепортироваться на координаты", (args, player) -> {
            if (!tp) {
                err("возможность телепортации отключена.", player);
                return;
            }
            float x, y;
            if (!Strings.canParseFloat(args[0]) || !Strings.canParseFloat(args[1])) {
                err("координаты должны быть целым числом!", player);
                return;
            } else {
                x = Float.parseFloat(args[0]);
                y = Float.parseFloat(args[1]);
            }
            if (x < 0 || x > world.width() || y < 0 || y > world.width()) {
                err("телепортироваться за границы мира - не самая лучшая идея.", player);
                return;
            }
            if (!player.unit().isFlying() && Vars.world.tileWorld(x, y).block() != Blocks.air) {
                err("переместиться в блок не получится!", player);
                return;
            }
            Unit oldUnit = player.unit();
            UnitType type = oldUnit.type;

            player.unit().kill();
            player.unit(type.spawn(player.team(), x*8, y*8));
            player.snapSync();
            player.unit().health = oldUnit.health;
            player.unit().ammo = oldUnit.ammo;
            player.unit().spawnedByCore = oldUnit.spawnedByCore;

            success("ты перемещен на координаты [accent]" + args[0] + ", " + args[1], player);
        });

        handler.<Player>register("vnw", "Голосование за скип волны", (args, player) -> {
            if (!vnw) {
                err("команда отключена.", player);
                return;
            }

            this.votes.add(player.uuid());
            int cur = this.votes.size();
            int req = (int) Math.ceil(ratio * Groups.player.size());
            Call.sendMessage("[cyan]VNW[]: [accent]" + player.name + "[] хочет скипнуть волну, [green]" + cur +
                    "[] голос(ов), [green]" + req + "[] необходимо.");

            if (cur < req) return;

            this.votes.clear();
            Call.sendMessage("[cyan]VNW[]: [green] голосование успешно, скипаем волну...");
            Vars.logic.runWave();
        });

        handler.<Player>register("spawn", "<юнит> <кол-во> <команда>", "Заспавнить юнитов.", (args, player) -> {
            if (!spawn) {
                err("возможность спавна юнита отключена.", player);
                return;
            }

            if (!adminCheck(player)) return;

            StringBuilder builder = new StringBuilder();
            int count;

            try {
               count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                err("количество юнитов должно быть числом!", player);
                return;
            }

            if (count > 25) {
                err("больше 25 юнитов заспавнить не получится!", player);
                return;
            }

            Team team;
            switch (args[2]) {
            	case "~": 
            	    team = player.team();
            	    break;
            	case "sharded": 
            	    team = Team.sharded;
            	    break;
            	case "blue": 
            	    team = Team.blue;
            	    break;
            	case "crux": 
            	    team = Team.crux;
            	    break;
            	case "derelict": 
	            team = Team.derelict;
	            break;
            	case "green": 
            	    team = Team.green;
            	    break;
            	case "purple": 
            	    team = Team.purple;
            	    break;
            	default: 
            	    err("команда не найдена!", player);
                    info("доступные команды: ", player);
            	    for (Team teamList : Team.baseTeams) builder.append(" - [accent]" + teamList.name + "[]\n");
            	    player.sendMessage(builder.toString());
            	    return;
            }

            UnitType unit = Vars.content.units().find(b -> b.name.equals(args[0]));
            if (unit == null) err("юнит не найден! Доступных юнитов можно узнать командой [lightgray]/units all[]!", player);
            else {            
                for (int i = 0; count > i; i++) {
                    unit.spawn(team, player.x, player.y);
                }
                success("ты заспавнил" + " [accent]" + count + " [accent]" + unit.name + " [lime]для команды [accent]" + team.name, player);
            }
        });

        handler.<Player>register("units", "[all/change] [юнит]", "Различные действия с юнитами", (args, player) -> {
            if(args.length == 0) {
                info("использование - [lightgray]/units all[cyan] или [lightgray]/units change[cyan].", player);
                return;
            } else if (args[0].equals("all")) {
                final String[] text = {""};
                Vars.content.units().each(unitType -> text[0] = String.format("%s%s[green]%s, ", text[0], unitType.emoji(), unitType.name));
                info("список всех юнитов: " + text[0], player);
            } else if (args[0].equals("change")) {
                if (!adminCheck(player)) return;

                UnitType founded = Vars.content.units().find(b -> b.name.equals(args[1]));
                if (founded == null) {
                    err("юнит не найден!", player);
                    info("доступных юнитов можно узнать командой [lightgray]/units all[]!", player);
                    return;
                }
                final Unit spawn = founded.spawn(player.team(), player.x(), player.y());
                spawn.spawnedByCore(true);
                player.unit(spawn);
                success(player);
            } else {
                err("использование - [lightgray]/units all[cyan] или [lightgray]/units change[cyan].", player);
            }
        });

        handler.<Player>register("uuid", "Узнать свой uuid", (args, player) -> info("твой uuid: [lime]" + player.uuid(), player));

        handler.<Player>register("map", "Информация о текущей карте", (args, player) -> info("ты играешь на карте: " + Vars.state.map.name() + "[white], которую сделал " + Vars.state.map.author(), player));

        handler.<Player>register("js", "<скрипт...>", "Запустить JavaScript код.", (args, player) -> {
            if (!adminCheck(player)) return;

            String output = mods.getScripts().runConsole(args[0]);
            err("> " + (isError(output) ? "[#ff341c]" + output : output), player);
        });

        handler.<Player>register("kill", "[@p|@a|@t|ID|Никнейм...]", "Совершить убийство. @a: все юниты. @p: все игроки. @t: все юниты в твоей команде", (arg, player) -> {
            if (!adminCheck(player)) return;
            if (arg.length == 0) {
                player.unit().kill();
                success("ты умер от депрессии. Помянем", player);
                return;
            }
            switch (arg[0]) {
                case "@p":
                    Groups.player.each(p -> p.unit().kill());
                    success("ты убил всех игроков", player);
                    return;
                case "@a":
                    Groups.unit.each(Unitc::kill);
                    success("ты убил всех юнитов", player);
                    return;
                case "@t":
                    Groups.unit.each(u -> {
                        if (u.team().equals(player.team())) u.kill();
                    });
                    success("ты убил всех юнитов в команде [accent]" + player.team().name, player);
                    return;
                default:
                    Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[0]) || p.uuid().equalsIgnoreCase(arg[0]));
                    if (target == null)
                        target = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[0].replaceAll("_", " ")));
                    if (target != null) {
                        target.unit().kill();
                        success("ты совершил убийство [accent]" + target.name, player);
                        info("на тебя было совершено успешное покушение. Ты сдох.", target);
                    } else {
                        err("игрок не существует или ты ввел неправильный аргумент.", player);
                    }
            }
        });

        handler.removeCommand("a");
        handler.removeCommand("t");

        handler.<Player>register("a", "<текст...>", "Отправить сообщение от имени админа", (args, player) -> {
            if (!adminCheck(player)) return;
            String playerName = NetClient.colorizeName(player.id, player.name);
            Groups.player.each(Player::admin, otherPlayer -> otherPlayer.sendMessage("<[scarlet]A[]> " + playerName + " [gold]>[#ff4449] " + args[0]));
        });

        handler.<Player>register("t", "<текст...>", "Отправить командное сообщение", (args, player) -> {
            String playerName = NetClient.colorizeName(player.id, player.name);
            Groups.player.each(o -> o.team() == player.team(), otherPlayer -> otherPlayer.sendMessage("<[#" + player.team().color + "]T[]> " + playerName + " [gold]>[lime] " + args[0]));
        });

        handler.<Player>register("rainbow", "Сделать никнейм радужным", (args, player) -> {          
            RainbowPlayerEntry old = rainbow.find(r -> r.player.uuid().equals(player.uuid()));
            if(old != null){
                rainbow.remove(old);
                player.name = Vars.netServer.admins.getInfo(player.uuid()).lastName;
                setPrefix(player);
                info("радужный никнейм выключен", player);
                return;
            }
            info("радуга запущена! Чтобы отключить радугу, перезайди на сервер или используй /rainbow повторно.", player);
            RainbowPlayerEntry entry = new RainbowPlayerEntry();
            entry.player = player;
            entry.stripedName = Strings.stripColors(player.name);
            rainbow.add(entry);
        });

        handler.<Player>register("report", "<ID|Никнейм> <Причина>", "Отправить жалобу на игрока. Абьюз запрещён.", (args, player) -> {
            Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(args[0]) || p.uuid().equalsIgnoreCase(args[0]));
            if(target == null) target = Groups.player.find(p -> p.name().equalsIgnoreCase(args[0].replaceAll("_", " ")));
            if(target == null) err("Чела нет или он сдох!", player);
            if(target.uuid().equals(player.uuid())) {
                warn("жалобы на себя запрещены!", player);
                return;
            } 
            sendReport(player.name(), args[1], target.name());
            success(player);
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("unban-all", "Разбанить всех", arg -> {
            netServer.admins.getBanned().each(unban -> netServer.admins.unbanPlayerID(unban.id));
            netServer.admins.getBannedIPs().each(ip -> netServer.admins.unbanPlayerIP(ip));
            Log.info("Все шизики разбанены!");
        });

        handler.register("rr", "Перезапустить сервер", args -> {
            Log.info("Reloading server...");
            System.exit(2);
        });

        handler.removeCommand("say");

        handler.register("say", "<сообщение...>", "Сказать от имени сервера.", arg -> {
            Call.sendMessage("[lime]Сервер[white]: " + arg[0]);
            Log.info("Сервер: " + arg[0]);
        });

        handler.register("enable", "<vnw/anarchy/spawn/tp/effects/prefixes>", "Включить нужную фичу.", arg -> {
            switch (arg[0]) {
                case ("vnw") -> {
                    vnw = true;
                    config.setJsonValue(jsonData, arg[0], true);
                    config.saveJsonData(jsonData);
                    Log.info("VNW включен!");
                }
                case ("anarchy") -> {
                    anarchy = true;
                    config.setJsonValue(jsonData, arg[0], true);
                    config.saveJsonData(jsonData);
                    Log.info("Режим anarchy включен!");
                }
                case ("spawn") -> {
                    spawn = true;
                    config.setJsonValue(jsonData, arg[0], true);
                    config.saveJsonData(jsonData);
                    Log.info("SpawnUnit включен!");
                }
                case ("tp") -> {
                    tp = true;
                    config.setJsonValue(jsonData, arg[0], true);
                    config.saveJsonData(jsonData);
                    Log.info("Телепорт включен!");
                }
                case ("effects") -> {
                    effects = true;
                    config.setJsonValue(jsonData, arg[0], true);
                    config.saveJsonData(jsonData);
                    Log.info("Эффекты включены!");
                }
                case ("prefixes") -> {
                    prefixes = true;
                    config.setJsonValue(jsonData, arg[0], true);
                    config.saveJsonData(jsonData);
                    Log.info("Префиксы включены!");
                }
                default -> Log.info("Неверный аргумент!");
            }
        });

        handler.register("disable", "<vnw/anarchy/spawn/tp/effects/prefixes>", "Выключить нужную фичу.", arg -> {
            switch (arg[0]) {
                case ("vnw") -> {
                    vnw = false;
                    config.setJsonValue(jsonData, arg[0], false);
                    config.saveJsonData(jsonData);
                    Log.info("VNW выключен!");
                }
                case ("anarchy") -> {
                    anarchy = false;
                    config.setJsonValue(jsonData, arg[0], false);
                    config.saveJsonData(jsonData);
                    Log.info("Режим anarchy выключен!");
                }
                case ("spawn") -> {
                    spawn = false;
                    config.setJsonValue(jsonData, arg[0], false);
                    config.saveJsonData(jsonData);
                    Log.info("SpawnUnit выключен!");
                }
                case ("tp") -> {
                    tp = false;
                    config.setJsonValue(jsonData, arg[0], false);
                    config.saveJsonData(jsonData);
                    Log.info("Телепорт выключен!");
                }
                case ("effects") -> {
                    effects = false;
                    config.setJsonValue(jsonData, arg[0], false);
                    config.saveJsonData(jsonData);
                    Log.info("Эффекты выключены!");
                }
                case ("prefixes") -> {
                    prefixes = false;
                    config.setJsonValue(jsonData, arg[0], false);
                    config.saveJsonData(jsonData);
                    Log.info("Префиксы выключены!");
                }
                default -> Log.info("Неверный аргумент!");
            }
        });
    }

    private boolean isError(String output) {
        try {
            String errorName = output.substring(0, output.indexOf(' ') - 1);
            Class.forName("org.mozilla.javascript." + errorName);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void err(String msg, Player player) {
        player.sendMessage("[scarlet] [orange]>[scarlet] Ошибка: " + msg);
    }

    public static void info(String msg, Player player) {
        player.sendMessage("[sky] [orange]>[cyan] Информация: [white]" + msg);
    }

    public static void warn(String msg, Player player) {
        player.sendMessage("[gold]⚠ [orange]>[gold] Предупреждение: [white]" + msg);
    }

    public static void success(String msg, Player player) {
        player.sendMessage("[lime] [orange]>[lime] Успешно: " + msg);
    }

    public static void success(Player player) {
        player.sendMessage("[lime] [orange]>[lime] Успешно!");
    }

    public static boolean adminCheck(Player player) {
        if (!player.admin()) {
            player.sendMessage("[scarlet] [orange]>[scarlet] Эту команду могут использовать только админы!");
            return false;
        }
        return true;
    }

    public void setPrefix(Player player) {
        if (!prefixes) return;
        String locale = player.locale().toUpperCase();
        locale = locale.substring(0, 2);
        if (player.uuid().equals("GYmJmGDY2McAAAAAN8z4Bg==")) {
            player.name = "[sky]Владелец[white] | [gold][" + locale + "][white]" + player.name();
        } else if (player.admin()) {
            player.name = "[scarlet]Админ[white] | [gold][" + locale + "][white]" + player.name();
        } else {
            player.name = "[cyan]Игрок[white] | [gold][" + locale + "][white]" + player.name();
        } 
    }
    private void sendReport(String author, String reason, String target){
        Webhook wh = new Webhook(hook);
        wh.setUsername("Репортящая кошкодевочка");
        wh.addEmbed(new Webhook.EmbedObject()
                .setTitle("РЕПОРТ")
                .addField("Отправил:",author,false)
                .addField("Причина:",reason,false)
                .addField("Нарушитель:",target,false)
                .setColor(new Color(0, 200, 220)))
        ;
        try {
            wh.execute();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            wh=null;
        }
    }
}
