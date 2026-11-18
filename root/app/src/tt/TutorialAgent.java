package tt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.uky.cs.nil.tt.*;
import edu.uky.cs.nil.tt.io.*;
import edu.uky.cs.nil.tt.world.*;

/**
 * A Tandem Tales agent that plays the totirla. This agent can play as either
 * role. It defers to {@link PlayerBevhavior} and {@link GameMasterBehavior}
 * when playing as the Player and Game Master respectively.
 * 
 * Both of these behaviors depend on a story graph to inform the next action.
 */
public class TutorialAgent extends Client {
	
	/** The default server URL used if none is provided */
	public static final String DEFAULT_URL = "tandemtales.net";
	
	/** The default network port used if none is provided */
	public static final int DEFAULT_PORT = Client.DEFAULT_PORT;
	
	/** The default delay if none is provided */
	private static final long DEFAULT_DELAY = 2000;

	/** Always delay for at least one second. */
	private static final long ONE_SECOND = 1000;
	
	/** The ID number of the next agent to be created */
	private static int nextID = 0;
	
	/** A unique ID number used to identify this agent */
	public final int id = nextID++;
	
	/** Max milliseconds this agent will wait before taking a turn */
	public final long delay;

	/** The random number generator used to make decisions */
	private final Random random;

	/** Used to determine the Game Master's next action. */
	public GameMasterBehavior gm;
	
	/** Used to determine the Player's next action. */
	public PlayerBehavior p;
	
	/**
	 * Creates a new tutorial agent with the given settings.
	 * 
	 * @param url the URL of the server to connect to
	 * @param port the network port to connect to the server on
	 * @param random the pseudo-random number generator used to decide how long to wait
	 * @param delay the max milliseconds to wait before taking a turn
	 */
	private TutorialAgent(String url, int port, Random random, long delay) {
		super("tutorial", "tutorial", null, null, url, port);
		this.random = random;
		this.delay = delay;
	}
	
	/**
	 * Creates a new tutorial agent with a random starting seed and the {@link
	 * #DEFAULT_DELAY default delay} time.
	 * 
	 * @param url the URL of the server to connect to
	 * @param port the network port to connect to the server on
	 */
	public TutorialAgent(String url, int port) {
		this(url, port, new Random(), DEFAULT_DELAY);
	}
	
	@Override
	public String toString() {
		return "Tutorial Agent " + id;
	}
	
	@Override
	protected void onConnect(Connect connect) {
		System.out.println(this + " has connected to the server.");
	}
	
	@Override
	protected void onStart(World world, Role role, State state) {
		System.out.println(this + " has started its session as the " + role + " in world \"" + world.getName() + "\".");
		if (getRole() == Role.GAME_MASTER)
			gm = new GameMasterBehavior("tutorial-game-master.zip", world.getActions());
		if (getRole() == Role.PLAYER)
			p = new PlayerBehavior("tutorial-player.zip", world);
	}
	
	@Override
	protected int onChoice(Status status) {
		// Sleep for a few seconds.
		// The tutorial is much easier to do when the computer
		// doesn't act instantly
		try {
			if(delay > 0)
				Thread.sleep(random.nextLong(delay) + ONE_SECOND);
		}
		catch(InterruptedException exception) {
			// If interrupted, return choice immediately.
		}
		if (getRole() == Role.GAME_MASTER) {
			// Decide whether to allow the proposal (if it is one.)
			if (isProposal(status))
				return gm.allowProposal(cleanHistory(), getRole(), getHistory().get(getHistory().size() - 1));
			// Defer to the Story Graph to determine the next action.
			int toTake = gm.recommendAction(cleanHistory(), getRole(), getChoices());
			return toTake;
		} 
		else if (getRole() == Role.PLAYER) {
			// Decide whether to allow the proposal (if it is one.)
			if (isProposal(status))
				return p.allowProposal(cleanHistory(), getHistory().get(getHistory().size() - 1), getState());
			// Defer to the Story Graph to determine the next action.
			int toTake = p.recommendAction(cleanHistory(), getChoices(), getState());
			return toTake;
		}
		// This code should never be reached.
		return getChoices().size() - 1;
	}

	/**
	 * A helper function that returns the history with all "passes" and 
	 * "failed actions" removed. This leaves a list of only actions that
	 * actually happened.
	 * @return the list of actions that happened in the story
	 */
	private List<Turn> cleanHistory() {
		List<Turn> clean = new ArrayList<Turn>();
		for (var t : getHistory()) {
			// We only get actions that are "successes."
			if (t.type.equals(Turn.Type.SUCCEED)) {
				clean.add(t);
			}
		}
		return clean;
	}
	
	/**
	 * Checks whether this agent has been asked to agree to (succeed) or disagree
	 * with (fail) a proposed joint action.
	 * 
	 * @param status the current status passed to this agent by the server
	 * @return true if the agent is currently responding to a proposal, or false
	 * if this is a normal turn
	 */
	private static final boolean isProposal(Status status) {
		List<Turn> choices = status.getChoices();
		return choices.size() == 2 &&
			choices.get(0).type == Turn.Type.SUCCEED &&
			choices.get(1).type == Turn.Type.FAIL;
	}
	
	@Override
	protected void onEnd(Ending ending) throws Exception {
		System.out.println(this + " has ended its session: \"" + ending.getDescription() + "\".");
	}
	
	@Override
	protected void onStop(String message) throws Exception {
		System.out.println(this + " has stopped" + (message == null ? "" : " because \"" + message + "\"") + ".");
	}
	
	@Override
	protected void onDisconnect() throws Exception {
		System.out.println(this + " has disconnected.");
	}
}