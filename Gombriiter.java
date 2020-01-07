package bot;

import gameLogic.*;
import java.util.*;

/**
 * Gombriiter is a pretty good bot based on breadth first search complemented
 * by a few special cases. It's not perfect since it makes some silly mistakes
 * when put in very special case situations, but competing against the best of
 * the available bots in 100 games test runs it averages at about 50% 1:st place
 * and 20-30% 2:nd and 3:rd place and < 10% 4:th place.
 *      Currently I think its biggest flaw is its behavious when trapped in small
 * spaces.
 *
 * @author Simon Gombrii april 2019
 */

public class Gombriiter implements Brain {

    private GameState gameState;

    /**
     * The best direction of movement is determined by a breadth first search
     * followed by a correction in case the chosen direction could possibly
     * result in a head to head collision with another snake.
     *
     * @param snake controlled by bot.
     * @param gameState The current state of the game.
     * @return the best calculated next direction of movement.
     */
    public Direction getNextMove(Snake snake, GameState gameState){
        this.gameState = gameState;
        Direction bestChoice = breadthFirstSearch(snake.getHeadPosition(), snake.getCurrentDirection());
        return resolveIfCollision(snake, bestChoice);
    }

    /**
     * The main pathfinding algorithm used by the bot to determine the best
     * direction of movement for the snake. A breadth first search is performed.
     * It ends whenever the closest fruit is sighted or when the search is
     * exhaused after covering the whole board.
     *      If the search finds a fruit it does a secondary search from the
     * fruits position in order to determine which direction would be favourable
     * continuing after the fruit, thus determining which angle is the best when
     * approaching the fruit. The primary search then traces the path back to
     * the snake to determine which initial move direction lead to the best path.
     * It then returns that direction.
     *      If the search is exhausted either because no fruit is present or
     * all fruits are inaccessible the last path which, by the nature of bradth
     * first searches, is the longest path is returned. The logic is that this
     * path has a high probability of leading into an open space in contrast to
     * a cramped one.
     *      The benefit of a breadth first search rather than moving straight to
     * the goal is that the snake doesn't need to directly encounter an obstacle
     * in order to change course but can determine the optimal path from the start.
     *
     * @param start The head position of the snake
     * @param currDir The previously moved direction of the snake
     * @return The best decided direcion of movement for the snake determined
     * by a breadth first search
     */
    private Direction breadthFirstSearch(Position start, Direction currDir){
        HashMap<Position,Position> visited = new HashMap<>();
        Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        visited.put(start,null);
        visited.put(oppositeDirection(currDir).calculateNextPosition(start), null);
        Position lastRemoved = null;
        while (!queue.isEmpty() && !encounteredFruit(visited.keySet())){
            Position current = lastRemoved = queue.remove();

            for (Direction dir: Direction.values()){
                Position next = dir.calculateNextPosition(current);

                if (!visited.containsKey(next) && !gameState.getBoard().isLethal(next)){
                    if (!(gameState.getBoard().hasFruit(next) && oppositeDirection(dir, secondarySearch(next)))) {
                        queue.add(next);
                        visited.put(next, current);
                    }
                }
            }
        }
        Position last = encounteredFruit(visited.keySet())?
                spaceWithFruit(visited.keySet()):lastRemoved;
        while (!visited.get(last).equals(start)){
            last = visited.get(last);
        }
        return Direction.getDirectionFromPositionToPosition(start , last);
    }

    /**
     * Method called by breadthFirstSearch whenever a path to a fruit has been
     * found. From the fruit popsition a breadh first search is made to determine
     * the best direction of movement after the fuit hypothetically has been
     * eaten by the snake. For a detailed description of the algorithm read doc
     * for beadthFirstSearch. If the most favourable direction of movement returned
     * by this method is in conflict with the main search's found path it tries
     * another one.
     *      This method is very similar to breadthFirstSearch and could be joined
     * into one, but is separated for readability.
     *
     * @param start The position of the located fruit
     * @return the best direction of movement in the hypothetical situation when
     * the fruit has just been eaten
     */
    private Direction secondarySearch(Position start){
        HashMap<Position,Position> visited = new HashMap<>();
        Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        Position lastRemoved = null;
        while (!queue.isEmpty() && !encounteredFruit(visited.keySet())){
            Position current = lastRemoved = queue.remove();

            for (Direction dir: Direction.values()){
                Position next = dir.calculateNextPosition(current);

                if (!next.equals(start) && !visited.containsKey(next) && !gameState.getBoard().isLethal(next)){
                    queue.add(next);
                    visited.put(next,current);
                }
            }
        }
        Position last = encounteredFruit(visited.keySet())?
                spaceWithFruit(visited.keySet()):lastRemoved;
        while (!visited.get(last).equals(start)){
            last = visited.get(last);
        }
        return Direction.getDirectionFromPositionToPosition(start, last);
    }

    /**
     * @param positions Set of positions.
     * @return if any position from positions contains fruit.
     */
    private boolean encounteredFruit(Set<Position> positions){
        return spaceWithFruit(positions) != null;
    }

    /**
     * @param positions Set of positions
     * @return the first position in positions to contain fruit
     */
    private Position spaceWithFruit(Set<Position> positions){
        for (Position pos: positions){
            if (gameState.getBoard().hasFruit(pos))
                return pos;
        }
        return null;
    }

    /**
     * Returns an array containing the four directions making it possible to
     * loop through them.
     *
     * @return an array containing the four directions
     */
    private Direction[] eachDirection(){
        return new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    }

    /**
     * @param dir Direction from which to find the opposite direction
     * @return the opposite direction of dir
     */
    private Direction oppositeDirection(Direction dir){
        return Direction.values()[(dir.ordinal()+2)%4];

    }

    /**
     * @param first A direction
     * @param second Another direction
     * @return if first and second are directions opposites
     */
    private boolean oppositeDirection(Direction first, Direction second){
        return first == oppositeDirection(second);
    }

    /**
     * Method that is used in case a direction of movement is found that of some
     * reaason doesn't look so good after all. The method returns the remaining
     * directions left after discounting the non-favourable direction and the
     * direction that would result in a 180 degrees turn for the snake.
     *
     * @param snakeDir Last movement direction of snake
     * @param badDir Direction of movement proven to be non-favourable
     * @return another non-lethal direction
     */
    private List<Direction> otherPossibleDirections(Direction snakeDir, Direction badDir){
        ArrayList<Direction> otherDirs = new ArrayList<>(2);
        for (Direction dir: Direction.values()){
            if (dir != badDir && dir != oppositeDirection(snakeDir))
                otherDirs.add(dir);
        }
        return otherDirs;
    }

    /**
     * Method called after a breadth first search to determine if the chosen
     * direction could potentially lead to a head to head collision with any
     * other snake. If that's the case a new risk free direction is determined.
     *
     * @param snake The snake
     * @param newDir The chosen direction
     * @return a new direction in case the cosen could end in a head to head
     * collision
     */
    private Direction resolveIfCollision(Snake snake, Direction newDir){
        Position currentPos = snake.getHeadPosition();
        if (headToHeadCollision(snake, newDir.calculateNextPosition(currentPos))){
            for (Direction dir: otherPossibleDirections(snake.getCurrentDirection(), newDir)){
                Position headedPos = dir.calculateNextPosition(currentPos);
                if (!gameState.getBoard().isLethal(headedPos) &&
                        !headToHeadCollision(snake,headedPos))
                    return dir;
            }
        }
        return newDir;
    }

    /**
     * Checks if movement to a sertain position could result in a head to head
     * collision with another snake. This is determined by examining the
     * neighbouring positions for other snake heads and their last direction of
     * movement.
     *
     * @param snake The snake
     * @param pos A position to which hypothetically move the snake
     * @return Wether or not a move to pos could potentially lead to a head to
     * head collision
     */
    private boolean headToHeadCollision(Snake snake, Position pos){
        List<Position> neighbours = pos.getNeighbours();
        for (Snake other: gameState.getSnakes()){
            if (!other.isDead() && other != snake && neighbours.contains(other.getHeadPosition()) &&
                    other.getCurrentDirection().calculateNextPosition(other.getHeadPosition()).equals(pos)){
                return true;
            }
        }
        return false;
    }
}
