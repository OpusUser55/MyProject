package dev.ooga.client.waypoint;

import net.minecraft.core.BlockPos;

/** A named spot on one server, in one dimension. */
public record Waypoint(String name, BlockPos pos, String dimension) {
}
