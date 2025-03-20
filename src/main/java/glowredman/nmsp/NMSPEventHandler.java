package glowredman.nmsp;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;

public class NMSPEventHandler {

    private Set<EntityPlayer> prevSleeping = new HashSet<>();

    @SubscribeEvent
    public void onWorldTick(WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote || world.provider.dimensionId != 0
            || event.phase == Phase.START
            || world.getWorldTime() % NMSP.checkInterval != 0) {
            return;
        }

        // check players
        Set<EntityPlayer> sleepingPlayers = new HashSet<>();
        int totalPlayers = 0;
        for (EntityPlayer player : world.playerEntities) {
            if (player.posY <= NMSP.yLevelMiners) {
                // the player is a miner -> skip
                continue;
            }
            totalPlayers++;
            if (player.isPlayerFullyAsleep()) {
                sleepingPlayers.add(player);
            }
        }

        int numSleepingPlayers = sleepingPlayers.size();
        float ratio = (float) numSleepingPlayers / totalPlayers;

        if (ratio < NMSP.requiredPlayers) {
            // not enough players are asleep

            Float ratioPercent = ratio * 100.0f;
            sendTransitionMessage(
                world,
                NMSP.enterBedMessage,
                sleepingPlayers,
                this.prevSleeping,
                numSleepingPlayers,
                totalPlayers,
                ratioPercent);
            sendTransitionMessage(
                world,
                NMSP.leaveBedMessage,
                this.prevSleeping,
                sleepingPlayers,
                numSleepingPlayers,
                totalPlayers,
                ratioPercent);

            prevSleeping = sleepingPlayers;
            return;
        }

        // wake up the players
        ((WorldServer) world).wakeAllPlayers();
        this.prevSleeping.clear();

        // advance to the next morning
        long l = world.getWorldInfo()
            .getWorldTime() + 24000L;
        world.getWorldInfo()
            .setWorldTime(l - l % 24000L);

        sendMessageInDimension(world, NMSP.getSkipNightMessage());
    }

    private static void sendTransitionMessage(World world, String message, Set<EntityPlayer> candidates,
        Set<EntityPlayer> exclude, Integer sleepingPlayers, Integer totalPlayers, Float ratioPercent) {
        if (message.isEmpty()) {
            return;
        }
        for (EntityPlayer player : candidates) {
            if (!exclude.contains(player)) {
                sendMessageInDimension(
                    world,
                    message,
                    player.getDisplayName(),
                    sleepingPlayers,
                    totalPlayers,
                    ratioPercent);
            }
        }
    }

    private static void sendMessageInDimension(World world, String message, Object... args) {
        for (EntityPlayer player : world.playerEntities) {
            player.addChatMessage(new ChatComponentText(String.format(message, args)));
        }
    }
}
