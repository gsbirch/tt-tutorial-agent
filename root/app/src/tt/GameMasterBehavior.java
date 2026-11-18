package tt;

import java.io.File;
import java.util.List;
import java.util.Objects;

import edu.uky.cs.nil.sg.Node;
import edu.uky.cs.nil.sg.StoryGraph;
import edu.uky.cs.nil.tt.Role;
import edu.uky.cs.nil.tt.world.Turn;

/**
 * This class provides the behavior of the GM tutorial agent.
 * It uses a specially created story graph that has exactly one GM
 * action for each possible state. Using the list of previous actions,
 * this behavior finds the correct state and suggests the appropriate 
 * next action.
 */
public class GameMasterBehavior {
    /** The story graph used to find the next action to take. */
    public StoryGraph sg;
    
    /** The list of actions possible in the world. */
    List<edu.uky.cs.nil.tt.world.Action> worldActions;

    /** The player's order. The GM won't brew until the player has ordered. */
    String playerOrder = "";
    
    /** The name of the brew tea action. */
    static final String BREW_TEA = "brew(Barista, Tea)";

    /** The name of the brew coffee action. */
    static final String BREW_COFFEE = "brew(Barista, Coffee)";

    /** The name of the order tea action. */
    static final String ORDER_TEA = "order(Player, Tea)";

    /** The name of the order coffee action. */
    static final String ORDER_COFFEE = "order(Player, Coffee)";

    /** The name of the tea object. */
    static final String TEA = "Tea";

    /** The name of the coffee object. */
    static final String COFFEE = "Coffee";
    
    /**
     * Construct a Game Master Behavior object.
     * 
     * @param filename the location of the story graph file
     * @param worldActions the available actions in the world
     */
    public GameMasterBehavior(String filename, List<edu.uky.cs.nil.tt.world.Action> worldActions) {
        // Load the story graph.
        try {
            sg = StoryGraph.from(new File(filename));
        }
        catch (Exception e) {
            System.err.println("Error opening the story graphs!");
        }
        this.worldActions = worldActions;
    }

    /**
     * Translates from a Tandem Tales Action to a Story Graph Action.
     * @param ttAction the Tandem Tales Action
     * @return the corresponding Story Graph action
     */
    public edu.uky.cs.nil.sg.Action ttTosgAction(edu.uky.cs.nil.tt.world.Action ttAction) {
        for (var sga : sg.actions) {
            if (ttAction.name.equals(sga.name)) {
                return sga;
            }
        }
        return null;
    }

    /**
     * Translates from a Story Graph Action to a Tandem Tales Action.
     * @param ttAction the Story Graph Action
     * @return the corresponding Tandem Tales action
     */
    public edu.uky.cs.nil.tt.world.Action sgTottAction(edu.uky.cs.nil.sg.Action sgAction) {
        for (var tta : worldActions) {
            if (sgAction.name.equals(tta.name)) {
                return tta;
            }
        }
        return null;
    }

    /**
     * Finds the Story Graph Action for the action within a Turn.
     * @param t a Turn object with an action
     * @return the corresponding Story Graph Action
     */
    public edu.uky.cs.nil.sg.Action findAction(Turn t) {
        return ttTosgAction(t.action);
    }

    /**
     * Find the node in the story graph that corresponds to taking the given
     * plan in the initial state. We don't need to do any fix history as the GM
     * can see all actions.
     * 
     * @param history the previous turns taken
     * @return the node in the graph corresponding to the current state
     */
    public Node getToState(List<Turn> history) {
        // Map the TT turns to SG actions.
        List<edu.uky.cs.nil.sg.Action> sgActions = history.stream().map(this::findAction).toList();
        // Get the initital state of the problem.
        Node n = sg.nodes.get(0);
        // Get the node after taking the sequnce of actions.
        return n.getAfter(sgActions);
    }

    /**
     * Decides the index of what action to take next. This does so by consulting the story graph
     * to find an action to take.
     * @param history the previous turns
     * @param choices the available choices
     * @return the index of the choice to take
     */
    public int recommendAction(List<Turn> history, List<Turn> choices) {
        // Update by what the player has most recently ordered.
        setPlayerOrder(history);

        // Get the node in the graph for the current state.
        Node n = getToState(history);

        // Special behavior for state 1.
        // This is because ordering is idempotent.
        if (n.getID() == 1) return state1Action(history, choices);
        
        // Look at outgoing edges to find the action to take.
        // There should only be one that the GM will take.
        edu.uky.cs.nil.tt.world.Action act = null;
        for (var edge : n.edges.temporal) {
            if (edge.tail.equals(n) && !edge.isPlayerOnly()) {
                act = sgTottAction(edge.label);
                break;
            }
        }

        // Use the found action to find the choice to make.
        if (act != null) {
            for (int i = 0; i < choices.size(); i++) {
                // We want the index of the action to take within choices.
                if (Objects.equals(choices.get(i).action, act))
                    return i;
            }
            // This warning is here in case the action in the story graph isn't one of the choices.
            // This should never happen in practice.
            System.out.println("Warning! The GM behavior was unable to find an action. Passing instead...");
        }
        // If an action wasn't found, pass.
        return choices.size() - 1;
    }

    /**
     * Defines some special behavior for state 1. In this state, the player could
     * order either coffee or tea. So this function makes the GM brew whatever the
     * player ordered. This is needed as the player's order is not part of the state.
     * 
     * @param history the previous turns
     * @param choices the available choices
     * @return the index of the choice to take
     */
    private int state1Action(List<Turn> history, List<Turn> choices) {
        // If player's order is tea, brew tea.
        if (playerOrder == TEA) {
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).action.name.equals(BREW_TEA))
                    return i;
            }
        }
        // If player's order is coffee, brew coffee.
        if (playerOrder == COFFEE) {
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).action.name.equals(BREW_COFFEE))
                    return i;
            }
        }
        // If the player hasn't ordered yet, wait for them to do so.
        return choices.size() - 1;
    }

    /**
     * Decides whether to accept a proposal. This does so by checking the story graph
     * to see if the action is present. If it is, then this will return the index to
     * ACCEPT. Otherwise, it will REJECT the proposal.
     * @param history the previous turns
     * @param proposal the proposed action
     * @return 0 if the propsal should be accepted, 1 otherwise
     */
    public int allowProposal(List<Turn> history, Turn proposal) {
        // Update by what the player has most recently ordered
        setPlayerOrder(history);

        // Get the node in the graph for the current state.
        Node n = getToState(history);
        // We only accept actions that are in the state graph
        for (var edge : n.edges.temporal) {
            if (edge.tail.equals(n) && edge.label.name.equals(proposal.action.name)) {
                return 0;
            }
        }
        // If we could not find the action, reject it
        return 1;
    }

    /**
     * Finds what the player has most recently ordered.
     * @param history the previous turns
     */
    private void setPlayerOrder(List<Turn> history) {
        // We go through each action from most to least recent.
        for (int i = history.size() - 1; i >= 0; i--) {
            // If the player ordered tea last, set their order to tea.
            if (history.get(i).action.name.equals(ORDER_TEA)) {
                playerOrder = TEA;
                return;
            }
            // If the player ordered coffee last, set their order to coffee.
            if (history.get(i).action.name.equals(ORDER_COFFEE)) {
                playerOrder = COFFEE;
                return;
            }
        }
    }
}
