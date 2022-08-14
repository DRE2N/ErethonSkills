package de.erethon.spellbook.utils;

import org.bukkit.block.BlockFace;

/**
 * @author Fyreum
 */
public enum BlockFaceWrapper {

    NORTH_WEST(BlockFace.NORTH_WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_NORTH_WEST),
    NORTH_NORTH_WEST(BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH),
    NORTH(BlockFace.NORTH, BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH_NORTH_EAST),
    NORTH_NORTH_EAST(BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH, BlockFace.NORTH_EAST),
    NORTH_EAST(BlockFace.NORTH_EAST, BlockFace.NORTH_NORTH_EAST, BlockFace.EAST_NORTH_EAST),
    EAST_NORTH_EAST(BlockFace.EAST_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST),
    EAST(BlockFace.EAST, BlockFace.EAST_NORTH_EAST, BlockFace.EAST_SOUTH_EAST),
    EAST_SOUTH_EAST(BlockFace.EAST_SOUTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST),
    SOUTH_EAST(BlockFace.SOUTH_EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST),
    SOUTH_SOUTH_EAST(BlockFace.SOUTH_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH),
    SOUTH(BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_EAST, BlockFace.SOUTH_SOUTH_WEST),
    SOUTH_SOUTH_WEST(BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH, BlockFace.SOUTH_WEST),
    SOUTH_WEST(BlockFace.SOUTH_WEST, BlockFace.SOUTH_SOUTH_WEST, BlockFace.WEST_SOUTH_WEST),
    WEST_SOUTH_WEST(BlockFace.WEST_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST),
    WEST(BlockFace.WEST, BlockFace.WEST_SOUTH_WEST, BlockFace.WEST_NORTH_WEST),
    WEST_NORTH_WEST(BlockFace.WEST_NORTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST);

    private final BlockFace blockFace, left, right;

    BlockFaceWrapper(BlockFace blockFace, BlockFace left, BlockFace right) {
        this.blockFace = blockFace;
        this.left = left;
        this.right = right;
    }

    public boolean isSimilar(BlockFace facing) {
        return facing == blockFace || facing == left || facing == right;
    }

    public BlockFace getLeft() {
        return left;
    }

    public BlockFace getRight() {
        return right;
    }

    public static BlockFaceWrapper getByBlockFace(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case NORTH_EAST -> NORTH_EAST;
            case NORTH_WEST -> NORTH_WEST;
            case SOUTH_EAST -> SOUTH_EAST;
            case SOUTH_WEST -> SOUTH_WEST;
            case WEST_NORTH_WEST -> WEST_NORTH_WEST;
            case NORTH_NORTH_WEST -> NORTH_NORTH_WEST;
            case NORTH_NORTH_EAST -> NORTH_NORTH_EAST;
            case EAST_NORTH_EAST -> EAST_NORTH_EAST;
            case EAST_SOUTH_EAST -> EAST_SOUTH_EAST;
            case SOUTH_SOUTH_EAST -> SOUTH_SOUTH_EAST;
            case SOUTH_SOUTH_WEST -> SOUTH_SOUTH_WEST;
            case WEST_SOUTH_WEST -> WEST_SOUTH_WEST;
            default -> null;
        };
    }

    public static boolean isSimilar(BlockFace facing1, BlockFace facing2) {
        BlockFaceWrapper wrapper = getByBlockFace(facing1);
        return wrapper != null && wrapper.isSimilar(facing2);
    }
}
