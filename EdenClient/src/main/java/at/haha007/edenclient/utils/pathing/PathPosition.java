package at.haha007.edenclient.utils.pathing;

/**
 * Represents the index in the path and the progress in the current block
 *
 * @param block: index in the path, starts at 0 for the first block
 * @param progress: a value between 0 and 1, where 0 is the start of the current block and 1 is the end of the current block
 * @param distance: the distance to the current block
 */
public record PathPosition(int block, double progress, double distance) {
}
