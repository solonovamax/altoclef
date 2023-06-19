package adris.altoclef.chains;

import adris.altoclef.Debug;
import adris.altoclef.mixins.DeathScreenAccessor;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.time.TimerGame;
import adris.altoclef.util.time.TimerReal;
import gay.solonovamax.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public class DeathMenuChain extends TaskChain {
    // Sometimes we fuck up, so we might want to retry considering the death screen.
    private final TimerReal _deathRetryTimer = new TimerReal(8);
    private final TimerGame _reconnectTimer = new TimerGame(1);
    private final TimerGame _waitOnDeathScreenBeforeRespawnTimer = new TimerGame(2);
    private ServerInfo _prevServerEntry = null;
    private boolean _reconnecting = false;
    private int _deathCount = 0;
    private Class _prevScreen = null;


    public DeathMenuChain(TaskRunner runner) {
        super(runner);
    }

    private boolean shouldAutoRespawn(AltoClef mod) {
        return mod.getModSettings().isAutoRespawn();
    }

    private boolean shouldAutoReconnect(AltoClef mod) {
        return mod.getModSettings().isAutoReconnect();
    }

    @Override
    protected void onStop() {

    }

    @Override
    public void onInterrupt(TaskChain other) {

    }

    @Override
    protected void onTick() {

    }

    @Override
    public float getPriority() {
        // MinecraftClient.getInstance().getCurrentServerEntry().address;
        //        MinecraftClient.getInstance().
        Screen screen = MinecraftClient.getInstance().currentScreen;

        // This might fix Weird fail to respawn that happened only once
        if (_prevScreen == DeathScreen.class) {
            if (_deathRetryTimer.elapsed()) {
                Debug.logMessage("(RESPAWN RETRY WEIRD FIX...)");
                _deathRetryTimer.reset();
                _prevScreen = null;
            }
        } else {
            _deathRetryTimer.reset();
        }
        // Keep track of the last server we were on so we can re-connect.
        if (AltoClef.inGame()) {
            _prevServerEntry = MinecraftClient.getInstance().getCurrentServerEntry();
        }

        if (screen instanceof DeathScreen) {
            if (_waitOnDeathScreenBeforeRespawnTimer.elapsed()) {
                _waitOnDeathScreenBeforeRespawnTimer.reset();
                if (shouldAutoRespawn(AltoClef.INSTANCE)) {
                    _deathCount++;
                    Debug.logMessage("RESPAWNING... (this is death #" + _deathCount + ")");
                    assert MinecraftClient.getInstance().player != null;
                    String deathmessage = ((DeathScreenAccessor) screen).getMessage().getString(); //"(not implemented yet)"; //screen.children().toString();
                    MinecraftClient.getInstance().player.requestRespawn();
                    MinecraftClient.getInstance().setScreen(null);
                    for (String i : AltoClef.INSTANCE.getModSettings().getDeathCommand().split(" & ")) {
                        String command = i.replace("{deathmessage}", deathmessage);
                        String prefix = AltoClef.INSTANCE.getModSettings().getCommandPrefix();
                        while (MinecraftClient.getInstance().player.isAlive()) ;
                        if (command != "") {
                            if (command.startsWith(prefix)) {
                                AltoClef.INSTANCE.getCommandExecutor().execute(command, () -> {
                                }, e -> {
                                    e.printStackTrace();
                                });
                            } else if (command.startsWith("/")) {
                                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command.substring(1));
                            } else {
                                MinecraftClient.getInstance().player.networkHandler.sendChatMessage(command);
                            }
                        }
                    }
                } else {
                    // Cancel if we die and are not auto-respawning.
                    AltoClef.INSTANCE.cancelUserTask();
                }
            }
        } else {
            if (AltoClef.inGame()) {
                _waitOnDeathScreenBeforeRespawnTimer.reset();
            }
            if (screen instanceof DisconnectedScreen) {
                if (shouldAutoReconnect(AltoClef.INSTANCE)) {
                    Debug.logMessage("RECONNECTING: Going to Multiplayer Screen");
                    _reconnecting = true;
                    MinecraftClient.getInstance().setScreen(new MultiplayerScreen(new TitleScreen()));
                } else {
                    // Cancel if we disconnect and are not auto-reconnecting.
                    AltoClef.INSTANCE.cancelUserTask();
                }
            } else if (screen instanceof MultiplayerScreen && _reconnecting && _reconnectTimer.elapsed()) {
                _reconnectTimer.reset();
                Debug.logMessage("RECONNECTING: Going ");
                _reconnecting = false;

                if (_prevServerEntry == null) {
                    Debug.logWarning("Failed to re-connect to server, no server entry cached.");
                } else {
                    MinecraftClient client = MinecraftClient.getInstance();
                    ConnectScreen.connect(screen, client, ServerAddress.parse(_prevServerEntry.address), _prevServerEntry);
                    //client.setScreen(new ConnectScreen(screen, client, _prevServerEntry));
                }
            }
        }
        if (screen != null)
            _prevScreen = screen.getClass();
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getName() {
        return "Death Menu Respawn Handling";
    }
}
