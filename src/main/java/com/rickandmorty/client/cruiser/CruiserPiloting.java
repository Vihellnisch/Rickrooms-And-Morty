package com.rickandmorty.client.cruiser;

import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/** Reads the pilot's controls into the cruiser they're flying (on their own client, like a boat). */
public final class CruiserPiloting {
    private CruiserPiloting() {
    }

    public static void steer(SpaceCruiserEntity cruiser, Player pilot) {
        if (!(pilot instanceof LocalPlayer local)) return;
        boolean down = CruiserControls.DESCEND.isDown();
        cruiser.fly(local.input.forwardImpulse, local.input.leftImpulse, local.input.jumping, down, local.getYRot());
    }
}
