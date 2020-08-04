package app;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import game.HuntState;
import game.Hunter;
import game.Node;
import game.NodeStatus;
import game.ScramState;

/** A solution with huntOrb optimized and scram getting out as fast as possible. */
public class Pollack extends Hunter {

    /** A hashset keeping track of visited nodes. */
    private HashSet<Long> vset= new HashSet<>();

    /** Get to the orb in as few steps as possible. <br>
     * Once you get there, you must return from the function in order to pick it up. <br>
     * If you continue to move after finding the orb rather than returning, it will not count.<br>
     * If you return from this function while not standing on top of the orb, it will count as a
     * failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the orb in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the orb at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToOrb() in HuntState.<br>
     * You know you are standing on the orb when distanceToOrb() is 0.
     *
     * Use function moveTo(long id) in HuntState to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the orb, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first search. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void huntOrb(HuntState state) {
        // TODO 1: Get the orb

        dfsWalk(state);

    }

    /** Get out the cavern before the ceiling collapses, trying to collect as <br>
     * much gold as possible along the way. Your solution must ALWAYS get out <br>
     * before time runs out, and this should be prioritized above collecting gold.
     *
     * You now have access to the entire underlying graph, which can be accessed <br>
     * through ScramState. <br>
     * currentNode() and getExit() will return Node objects of interest, and <br>
     * getNodes() will return a collection of all nodes on the graph.
     *
     * Note that the cavern will collapse in the number of steps given by <br>
     * getStepsRemaining(), and for each step this number is decremented by the <br>
     * weight of the edge taken. <br>
     * Use getStepsRemaining() to get the time still remaining, <br>
     * pickUpGold() to pick up any gold on your current tile <br>
     * (this will fail if no such gold exists), and <br>
     * moveTo() to move to a destination node adjacent to your current node.
     *
     * You must return from this function while standing at the exit. <br>
     * Failing to do so before time runs out or returning from the wrong <br>
     * location will be considered a failed run.
     *
     * You will always have enough time to scram using the shortest path from the <br>
     * starting position to the exit, although this will not collect much gold. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the exit <br>
     * is a good starting solution */
    @Override
    public void scram(ScramState state) {
        // TODO 2: Get out of the cavern before it collapses, picking up gold along the way

        scrammer(state);
    }

    /** Returns when Pollack is standing on the orb given by HunterState s. Visits every tile
     * reachable along paths of unvisited tiles from start or current tile. Ends with Pollack
     * standing on orb. Precondition: start tile is unvisited. */
    private void dfsWalk(HuntState s) {
        if (s.distanceToOrb() == 0) { return; }
        long curr_id= s.currentLocation();
        vset.add(curr_id);
        Heap min_heap= heapifyNeigh(s);
        while (min_heap.size() != 0) {
            long shorter_neigh= (long) min_heap.poll();
            if (!vset.contains(shorter_neigh)) {
                vset.add(shorter_neigh);
                s.moveTo(shorter_neigh);
                if (s.distanceToOrb() == 0) { return; }
                dfsWalk(s);
                if (s.distanceToOrb() == 0) { return; }
                s.moveTo(curr_id);
            }
        }
    }

    /** Returns when Pollack gets to the exit before the cavern collapses. Finds the shortest path
     * to the exit and... */
    private void scrammer(ScramState s) {
        if (s.currentNode() == s.getExit()) { return; }
//        List<Node> shortest_path= Path.shortest(s.currentNode(), s.getExit());
//        for (Node n : shortest_path) {
//            if (n != s.currentNode()) { s.moveTo(n); }

        List<Node> max_gold= ShortestToMaxGold(s);
        for (Node n : max_gold) {
            if (s.currentNode() == s.getExit()) { return; }
            if (n != s.currentNode()) { s.moveTo(n); }
        }
        return;
    }

    /** Creates a min-heap with the neighbors of current node, where the priorities are the
     * distances from the orb. */
    private Heap<Long> heapifyNeigh(HuntState s) {
        Heap<Long> min_heap= new Heap(false);
        for (NodeStatus neigh : s.neighbors()) {
            min_heap.add(neigh.getId(), neigh.getDistanceToTarget());
        }
        return min_heap;

    }

    private Heap<Node> heapifyGold(ScramState s) {
        Heap<Node> maxgold_heap= new Heap(true);
        for (Node n : s.allNodes()) {
            maxgold_heap.add(n, n.getTile().gold());
        }
        return maxgold_heap;
    }

    /** Returns a list of nodes to the nodes containing the largest gold amount and to the exit
     * using less than the step limit . */
    private List<Node> ShortestToMaxGold(ScramState s) {
        Heap<Node> maxgold_heap= heapifyGoldB(s);
        List<Node> curr_path= new LinkedList<>();
        Node curr_node= s.currentNode();
        int steps= s.stepsLeft();
        while (maxgold_heap.size() != 0) {
            Node max_curr= maxgold_heap.poll();
            // maxgold_heap= heapifyGold(s);
            List<Node> shortest_gold= Path.shortest(curr_node, max_curr);
            List<Node> maxgold_toExit= Path.shortest(max_curr, s.getExit());
            if (Path.pathSum(shortest_gold) +
                Path.pathSum(maxgold_toExit) < steps) {
                curr_path.addAll(shortest_gold);
                curr_node= max_curr;
                steps= steps - Path.pathSum(shortest_gold);
            }
        }
        List<Node> curr_toExit= Path.shortest(s.currentNode(), s.getExit());
        curr_path.addAll(curr_toExit);
        return curr_path;

    }

    /** Creates a max-heap with the of all nodes, where the priorities are the gold to distance
     * ratio. */
    private Heap<Node> heapifyGoldB(ScramState s) {
        Heap<Node> max_heap= new Heap<>(true);
        for (Node neigh : s.allNodes()) {
            List<Node> shortest= Path.shortest(s.currentNode(), neigh);
            int dist= Path.pathSum(shortest);
            if (dist == 0) { dist= 1; }
            double goldToDist= neigh.getTile().gold() / dist;
            max_heap.add(neigh, goldToDist);
        }
        return max_heap;

    }

//    private Heap<Node> heapifyGoldC(ScramState s) {
//        Heap<Node> max_goldheap= heapifyGold(s);
//        int mid= heapifyGold(s).size / 2;
//        Heap<Node> max1_heap= new Heap<>(true);
//        for (int i= 0; i < mid; i++ ) {
//            double curr_gold= max_goldheap.b[0].priority;
//            Node curr_max= max_goldheap.poll();
//            max1_heap.add(curr_max, curr_gold);
//        }
//        Heap<Node> max2_heap= new Heap<>(true);
//        for (int i= mid; i < max_goldheap.size(); i++ ) {
//            double curr_gold= max_goldheap.b[0].priority;
//            Node curr_max= max_goldheap.poll();
//            max2_heap.add(curr_max, curr_gold);
//        }
//
//        for (Node neigh : max1_heap) {
//            List<Node> shortest= Path.shortest(s.currentNode(), neigh);
//            int dist= Path.pathSum(shortest);
//            if (dist == 0) { dist= 1; }
//            double goldToDist= neigh.getTile().gold() / dist;
//            max_heap.add(neigh, goldToDist);
//        }
//        }
//        return max_heap;
//
//    }

}
