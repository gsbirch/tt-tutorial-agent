package tt;

import edu.uky.cs.nil.tt.ClientFactory;

/**
 * A factory that continuously creates {@link TutorialAgent}s as needed.
 */
public class TutorialAgentFactory extends ClientFactory {
    
    /** The URL of the server */
    private final String url;
    
    /** The port number of the server */
    private final int port;
    
    /**
     * Creates a new tutorial agent factory that connects to a Tandem Tales server
     * on the given URL and port number.
     * 
     * @param url the URL of the server
     * @param port the port number of the server
     */
    public TutorialAgentFactory(String url, int port) {
        this.url = url;
        this.port = port;
    }
    
    @Override
    public String toString() {
        return "Tutorial Agent Factory";
    }
    
    @Override
    protected void onStart() throws Exception {
        System.out.println(this + " has started.");
    }
    
    @Override
    protected TutorialAgent create() {
        return new TutorialAgent(url, port);
    }
    
    @Override
    protected void onClose() throws Exception {
        System.out.println(this + " has been closed.");
    }
    
    @Override
    protected void onStop() throws Exception {
        System.out.println(this + " has stopped.");
    }
}