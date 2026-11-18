package tt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import edu.uky.cs.nil.sg.Node;
import edu.uky.cs.nil.sg.StoryGraph;
import edu.uky.cs.nil.tt.Role;
import edu.uky.cs.nil.tt.world.Constant;
import edu.uky.cs.nil.tt.world.State;
import edu.uky.cs.nil.tt.world.Turn;
import edu.uky.cs.nil.tt.world.Value;
import edu.uky.cs.nil.tt.world.Variable;
import edu.uky.cs.nil.tt.world.World;

/**
 * This class provides the behavior of the player tutorial agent.
 * It uses a specially created story graph that has usually has exactly 
 * one player action for each possible state. Using the list of previous 
 * actions, this behavior finds the correct state and suggests the appropriate 
 * next action.
 * 
 * Sometimes there is more than one action, this is to incorporate any actions the
 * player may consent to if the GM proposes them. For example, if the Barista just
 * offers the tea before payment, the player will accept.
 */
public class PlayerBehavior {
    /** The story graph used to find the next action to take. */
    public StoryGraph sg;

    /** The world object. */
    World world;

    /** The list of actions possible in the world. */
    List<edu.uky.cs.nil.tt.world.Action> worldActions;

    /** The name of the trade action. */
    static final String TRADE_ACTION = "trade(Player, Money, Barista, Tea)";

    /** The name of the brew tea action. */
    static final String BREW_TEA = "brew(Barista, Tea)";

    /** The name of the brew coffee action. */
    static final String BREW_COFFEE = "brew(Barista, Coffee)";

    /** The coffee location fluent. */
    static final String COFFEE_LOCATION = "has(Coffee)";

    /** The tea location fluent. */
    static final String TEA_LOCATION = "has(Tea)";

    /** The Barista location value. */
    static final String BARISTA_VALUE = "Barista";

    /** The world Variable representing the coffee's location. */
    Variable coffeeLoc;

    /** The world Variable representing the tea's location. */
    Variable teaLoc;

    /** The world Value representing the Barista location. */
    Value baristaVal;
    
    /**
     * Construct a Player Behavior object.
     * 
     * @param filename the location of the story graph file
     * @param world the world object for the tutorial
     */
    public PlayerBehavior(String filename, World world) {
        // Load the story graph.
        try {
            sg = StoryGraph.from(new File(filename));
            System.out.println("Successfully loaded the story graph!");
        }
        catch (Exception e) {
            System.err.println("Error opening the story graphs!");
        }
        this.worldActions = world.getActions();
        this.world = world;

        // Grab the location variables and Barista value.
        coffeeLoc = world.getVariable(COFFEE_LOCATION);
        teaLoc = world.getVariable(TEA_LOCATION);
        baristaVal = world.getEntity(BARISTA_VALUE);
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
     * Finds the Story Graph Action with the corresponding name.
     * @param s the name of an action
     * @return the action with that name
     */
    public edu.uky.cs.nil.sg.Action findActionSG(String s) {
        for (var a : sg.actions) {
            if (a.name.equals(s))
                return a;
        }
        return null;
    }

    /**
     * Finds the Tandem Tales Action with the corresponding name.
     * @param s the name of an action
     * @return the action with that name
     */
    public edu.uky.cs.nil.tt.world.Action findActionTT(String s) {
        for (var a : worldActions) {
            if (a.name.equals(s))
                return a;
        }
        return null;
    }

    /**
     * Find the node in the story graph that corresponds to taking the given
     * plan in the initial state. Also fixes the history by adding brew actions
     * if the player didn't see them
     * 
     * @param history the previous turns taken
     * @param state the current state of the problem (for history fixing)
     * @return the node in the graph corresponding to the current state
     */
    public Node getToState(List<Turn> history, State state) {
        List<edu.uky.cs.nil.sg.Action> sgActions = history.stream().map(this::findAction).collect(Collectors.toCollection(ArrayList::new));
        // Fix the history:
        // If the barista brewed anything while the player was outside,
        // They "don't know the barista brewed anything",
        // So those actions need to be added manually/
        if (!state.get(coffeeLoc).equals(Constant.NULL) && !sgActions.contains(findActionSG(BREW_COFFEE))) {
            sgActions.add(0, findActionSG(BREW_COFFEE));
        }
        if (!state.get(teaLoc).equals(Constant.NULL) && !sgActions.contains(findActionSG(BREW_TEA))) {
            sgActions.add(0, findActionSG(BREW_TEA));
        }
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
     * @param state the current state
     * @return the index of the choice to take
     */
    public int recommendAction(List<Turn> history, List<Turn> choices, State state) {
        // Get the node in the graph for the current state.
        Node n = getToState(history, state);
        
        // Look at outgoing edges to find the action to take.
        edu.uky.cs.nil.tt.world.Action act = null;
        for (var edge : n.edges.temporal) {
            // When it's available, the player will ALWAYS ask to trade.
            // Otherwise, the player will typical enter/leave the shop, or order tea.
            if (edge.tail.equals(n) && edge.label.name.equals(TRADE_ACTION)) {
                act = sgTottAction(edge.label);
                break;
            }
            // We want actions where the player consents.
            if (act == null && edge.tail.equals(n) && edge.isPlayer()) {
                act = sgTottAction(edge.label);
                // We don't break because we want to keep searching for
                // the trade action.
            }
        }

        // Use the found action to find the choice to make.
        if (act != null) {
            // We want the index of the action to take within choices.
            for (int i = 0; i < choices.size(); i++) {
                if (Objects.equals(choices.get(i).action, act))
                    return i;
            }
            // This warning is here in case the action in the story graph isn't one of the choices.
            // This should never happen in practice.
            System.out.println("Warning! The player behavior was unable to find an action. Passing instead...");
        }
        // If an action wasn't found, pass.
        return choices.size() - 1;
    }

    /**
     * Decides whether to accept a proposal. This does so by checking the story graph
     * to see if the action is present. If it is, then this will return the index to
     * ACCEPT. Otherwise, it will REJECT the proposal.
     * @param history the previous turns
     * @param proposal the proposed action
     * @param state the current state
     * @return 0 if the propsal should be accepted, 1 otherwise
     */
    public int allowProposal(List<Turn> history, Turn proposal, State state) {
        // Get the node in the graph for the current state.
        Node n = getToState(history, state);
        // We only accept actions that are in the state graph.
        for (var edge : n.edges.temporal) {
            if (edge.tail.equals(n) && edge.label.name.equals(proposal.action.name)) {
                return 0;
            }
        }
        // If we could not find the action, reject it.
        return 1;
    }

}
