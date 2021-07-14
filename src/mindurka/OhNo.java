package mindurka;

import java.util.HashSet;

import arc.*;
import arc.util.*;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.Vars;

public class OhNo extends Plugin {

    private static double mapratio = 0.6;   
    private HashSet<String> mapvotes = new HashSet<>();
    private static double waveratio = 0.3;   
    private HashSet<String> wavevotes = new HashSet<>();

    public void init() {
        Events.on(PlayerLeave.class, e -> {
            Player player = e.player;
            int mapcur = this.mapvotes.size();
            int mapreq = (int) Math.ceil(mapratio * Groups.player.size());
            int wavecur = this.wavevotes.size();
            int wavereq = (int) Math.ceil(waveratio * Groups.player.size());
            if(mapvotes.contains(player.uuid())) {
                mapvotes.remove(player.uuid());
                Call.sendMessage("[cyan]MAP SKIPPER[]: [accent]" + player.name + "[accent] has disconnected, [green]" + mapcur + "[] votes, [green]" + mapreq + "[] required.");
            }
            if(wavevotes.contains(player.uuid())) {
                wavevotes.remove(player.uuid());
                Call.sendMessage("[sky]WAVE SKIPPER[]: [accent]" + player.name + "[accent] has disconnected, [green]" + wavecur + "[] votes, [green]" + wavereq + "[] required.");
            }
        });
        // clear votes on game over and new wave
        Events.on(GameOverEvent.class, e -> {
            this.mapvotes.clear();
            this.wavevotes.clear();
        });
    }


    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){

        //register a simple reply command
        handler.<Player>register("skip", "<map/wave>", "Vote For Skipping map/waves | Голосование за скип волны", (args, player) -> {
            switch (args[0]) {
                case "map":
                    mapvotes.add(player.uuid());
                    int mapcur = this.mapvotes.size();
                    int mapreq = (int) Math.ceil(mapratio * Groups.player.size());
                    Call.sendMessage("[cyan]MAP SKIPPER[]: [accent]" + player.name + "[accent] wants to skip the map, [green]" + mapcur + "[] votes, [green]" + mapreq + "[] required.");
                    if (mapcur < mapreq) {
                        return;
                    }

                    this.mapvotes.clear();
                    Call.sendMessage("[cyan]MAP SKIPPER[]: [green] vote passed, skipping the map...");
                    Events.fire(new GameOverEvent(Team.derelict));
                    break;
                case "wave":
                    wavevotes.add(player.uuid());
                    int wavecur = this.wavevotes.size();
                    int wavereq = (int) Math.ceil(waveratio * Groups.player.size());
                    Call.sendMessage("[sky]WAVE SKIPPER[]: [accent]" + player.name + "[accent] wants to skip the wave, [green]" + wavecur + "[] votes, [green]" + wavereq + "[] required.");
                    if (wavecur < wavereq) {
                        return;
                    }

                    this.wavevotes.clear();
                    Call.sendMessage("[sky]WAVE SKIPPER[]: [green] vote passed, skipping the wave...");
                    Vars.logic.runWave();
                    break;
                default:
                    player.sendMessage("[scarlet]ERROR: invalid argument. [accent]Usage: [lightgray]/skip map/wave");
            }
        });
    }
}
