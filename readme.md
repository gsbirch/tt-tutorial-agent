# Tandem Tales Tutorial Agent

This is a [Tandem Tales](https://sgware.github.io/tt) agent that is designed to play the tutorial. It can play as the player or the game master.

This agent works using two separate custom [Story Graphs](https://sgware.com/story-graph) for the player and game master. The Story Graphs are highly pruned versions of the tutorial world that typically
hold one action for the agent's role in any given state. Thus, when the agent finds the story in that state on their turn, they will take the action directed by the Story Graph.

## Running The Agent

Like all Tandem Tales agent, this project has a [Docker image](Dockerfile) that defines a container containing all the needed components and libraries to run the agent. There are no dependencies required on your device besides [Docker](https://www.docker.com).

To download and run this agent alongside a local instance of the Tandem Tales
web server, open a console and type:
```
git clone https://github.com/gsbirch/tt-tutorial-agent.git
cd tt-tutorial-agent
docker compose build
docker compose up
```
Then open a web browser and go to [http://localhost](http://localhost) to see a
local version of the Tandem Tales website running on your computer where you can
play an interactive story with this agent. You will likely see a security
warning because the traffic is encrypted with a self-signed certificate. It is
safe to ignore this warning because everything is running locally on your
computer.

This agent can also be ran alone, typically to connect to the [Tandem Tales Website](https://tandemtales.net/).
```
docker build -t tt-tutorial-agent .
docker run tt-tutorial-agent
```
Make sure you update the url to connect to.

## Story Graph Navigation

The tutorial agent defers to one of two classes, based on the role the agen will take:
- The [Game Master Behavior](/root/app/src/tt/GameMasterBehavior.java), which loads the [GM Story Graph](/root/app/tutorial-game-master.zip)
- The [Player Behavior](/root/app/src/tt/PlayerBehavior.java), which loads the [Player Story Graph](/root/app/tutorial-player.zip)

When the agent must choose an action to take, it takes the following general steps (with some differences noted later):
1. Uses the previous turns in [getHistory()](https://sgware.com/tt-client-java/edu/uky/cs/nil/tt/Client.html#getHistory()) to find the [Story Graph State](https://sgware.com/story-graph/edu/uky/cs/nil/sg/State.html) in the [Story Graph](https://sgware.com/story-graph/edu/uky/cs/nil/sg/StoryGraph.html) corresponding to the current state of the world
2. Scans that state's [Temporal Edges'](https://sgware.com/story-graph/edu/uky/cs/nil/sg/TemporalEdge.html) [labels](https://sgware.com/story-graph/edu/uky/cs/nil/sg/TemporalEdge.html#label) to find a [Story Graph Action](https://sgware.com/story-graph/edu/uky/cs/nil/sg/Action.html) that the agent can propose. For the player, this is an action they can consent to. For the game master, this is any action that is not only for the player.
3. If no action is present, the agent passes their turn.
4. If there is an action present, the agent finds the corresponding [Tandem Tales Action](https://sgware.com/tt-server/edu/uky/cs/nil/tt/world/Action.html) for that [Story Graph Action](https://sgware.com/story-graph/edu/uky/cs/nil/sg/Action.html), and returns the index for that action within [getChoices()](https://sgware.com/tt-server/edu/uky/cs/nil/tt/Agent.html#getChoices())

When the agent must choose whether to accept a propsed [Tandem Tales Action](https://sgware.com/tt-server/edu/uky/cs/nil/tt/world/Action.html), it takes the following general steps:
1. Uses the previous turns in [getHistory()](https://sgware.com/tt-client-java/edu/uky/cs/nil/tt/Client.html#getHistory()) to find the [Story Graph State](https://sgware.com/story-graph/edu/uky/cs/nil/sg/State.html) in the [Story Graph](https://sgware.com/story-graph/edu/uky/cs/nil/sg/StoryGraph.html) corresponding to the current state of the world.
2. Scans that state's [Temporal Edges'](https://sgware.com/story-graph/edu/uky/cs/nil/sg/TemporalEdge.html) [labels](https://sgware.com/story-graph/edu/uky/cs/nil/sg/TemporalEdge.html#label) to find a [Story Graph Action](https://sgware.com/story-graph/edu/uky/cs/nil/sg/Action.html) that corresponds to the proposed action.
3. If there is such a label is present, the agent accepts the proposal.
4. If there is no such label present, the agent rejects the proposal.

## Game Master Behavior

The [Game Master Behavior](/root/app/src/tt/GameMasterBehavior.java) is straightforward. It will wait for the player to enter the shop and order either coffee or tea. It will then brew the requested drink and offer the drink to the player from some money. The player is not allowed to leave the shop until they have received a drink and paid for it.

## Player Behavior

The [Player Behavior](/root/app/src/tt/PlayerBehavior.java) is also straightforward. When given the chance to take a turn, the agent will choose to walk into the shop then order tea. Once the game master brews tea, the agent will always offer their money for the tea when given the chance to take a turn (until they have either). The player rejects any proposals that involve coffee. They will not leave the shop until they have received the tea and paid for it.

When the player finds the corresponding Story Graph State for the current state of the world, they must make a special consideration. If the game master brews a drink when the agent is outside of the shop, [getHistory()](https://sgware.com/tt-client-java/edu/uky/cs/nil/tt/Client.html#getHistory()) will not list the brew actions, as the agent didn't "see" it happen. However, they know whether the drinks have been brewed once they enter the shop. Thus, if the agent did not see the brew actions but sees one or both of the drinks, they will assume the corresponding brew actions happened before they entered the shop.

## Version History

- 1.0.0: First public release.

## License

The Tandem Tales Tutorial Agent was developed by Gage Birchmeier.
in the [Computer Science department](http://cs.uky.edu) at the
[University of Kentucky](http://uky.edu). Development was sponsored in part by
the [US National Science Foundation](http://www.nsf.gov),
([Grant #2145153](https://www.nsf.gov/awardsearch/show-award?AWD_ID=2145153))
and the [US Army Research Office](http://arl.devcom.army.mil/who-we-are/aro)
(Grant W911NF-24-1-0195).

The code in this project is free and open source. It is released under the
[GNU General Public License version 3.0](https://choosealicense.com/licenses/gpl-3.0/)
(GPL 3.0). You are free to share and modify this software, even for commercial
purposes, as long as you give credit to the original creators and you also
release your modifications under the GPL 3.0 license. See the
[license file](license.txt) for details. The University of Kentucky retains all
rights not specifically granted.

Docker, Docker Compose, and the software used in Docker containers have their
own licenses that are not necessarily covered by GPL-3.0.
