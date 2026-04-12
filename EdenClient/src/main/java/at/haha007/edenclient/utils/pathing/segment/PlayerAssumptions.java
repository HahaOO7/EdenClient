package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.PathingUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public record PlayerAssumptions(double width, double jumpPower, double walkingSpeed, double stepHeight) {
    public static PlayerAssumptions create() {
        LocalPlayer player = PlayerUtils.getPlayer();
        return new PlayerAssumptions(player.getDimensions(player.getPose()).width(),
                PathingUtils.getJumpPower(),
                PlayerUtils.getWalkingSpeed(),
                player.maxUpStep());
    }

    public boolean isGood(Player player) {
        float playerWidth = player.getDimensions(player.getPose()).width();
        return isRoughly(playerWidth, width)
                && isRoughly(player.maxUpStep(), stepHeight)
                && PathingUtils.getJumpPower() >= jumpPower
                && PlayerUtils.getWalkingSpeed() >= walkingSpeed;
    }

    private boolean isRoughly(double a, double b) {
        return a >= b * 0.99 && a <= b * 1.01;
    }
}
