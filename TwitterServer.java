import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * The TwitterServer class is an implementation of a server that let users
 * create, subscribe and unsubscribe to hashtags. The users can also post
 * one-line messages; if a message contains a hashtag to which another user is
 * subscribed, the message will be transfered by this server to the subscribed
 * user.
 */
public class TwitterServer implements Runnable {
    private ServerSocket serverSocket;
    // Map each hastags to a set of its subscribers (because we don't want
    // duplicates.)
    private Map<String, Set<BufferedWriter>> crntSubscriptions;
    public static final int NB_THREADS = 10; // Number of threaded instances of TwitterServer that will be launched
    // Server reply string when receiving unknown commands
    private static final String UNKNOWN_CMD = "ERROR: UNKNOWN COMMAND\n"
            + "Supported commands are: \n - SUBSCRIBE #<hashtag> [#hashtag...],\n"
            + " - UNSUBSCRIBE #<hashtag> [#hashtag...], \n - TWEET <one-line msg>";

    /**
     * TwitterServer constructor, requires a ServerSocket. Initializes the
     * crntSubscriptions Map.
     * 
     * @param ss SokcetServer
     */
    public TwitterServer(ServerSocket ss) {
        serverSocket = ss;
        crntSubscriptions = new HashMap<String, Set<BufferedWriter>>();
    }

    /**
     * The main method of the TwitterServer which accepts connections from a client
     * and then wait to receive one of the accepted commands. Accepted commands are
     * "SUBSCRIBE", "UNSUBSCRIBE" and "TWEET".
     */
    public void run() {
        while (true) {
            try {
                Socket s = serverSocket.accept();
                InetAddress inetAddr = s.getInetAddress();

                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                // Used to easily remove subscriptions when the client disconnect.
                Set<String> subs = new HashSet<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Make a set of all the hashtags in the message.
                    Set<String> msgHashtags = Arrays.stream(line.split(" "))
                            .filter(word -> word.startsWith("#") && word.length() > 1).collect(Collectors.toSet());

                    if (line.startsWith("SUBSCRIBE ")) {
                        addSubscription(msgHashtags, writer);
                        subs.addAll(msgHashtags);
                    } else if (line.startsWith("UNSUBSCRIBE ")) {
                        // Remove client from hashtag's list of subscibers (if present, otherwise do
                        // nothing)
                        msgHashtags.forEach(h -> {
                            synchronized (crntSubscriptions) {
                                Set<BufferedWriter> set;
                                if (crntSubscriptions.containsKey(h) && (set = crntSubscriptions.get(h)) != null) {
                                    set.remove(writer);
                                }
                            }
                        });
                    } else if (line.startsWith("TWEET ")) {
                        // First prepare the message to transfer to subscribers
                        String tweet = line.replaceFirst("TWEET ", "").trim();
                        syncPrintln(tweet);
                        transferTweet(tweet, msgHashtags, writer);
                    }
                    // The "UNKNOWN CMD" reply was removed because assignment specifications
                    // precised not to add supplementary outputs
                    /*
                     * else { synchronized (writer) { try { writer.write(UNKNOWN_CMD);
                     * writer.newLine(); writer.flush(); } catch (IOException e) {
                     * System.out.println(e); } } }
                     */
                }
                // Removing disconnected client from all its subscriptions
                subs.forEach(h -> crntSubscriptions.get(h).remove(writer));
                reader.close();
                writer.close();
                s.close();
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    /**
     * This method transfers a received tweet to every client subscribed to at least
     * one of the hashtags contained in the msgHashtags parameter. A tweet is not
     * transfered to its creator even if it is subscribed to one of the hashtags. A
     * tweet is never transfered more than once to a given client even if the said
     * client is subscribed to multiple hashtags contained in msgHashtags.
     * 
     * @param tweet       the message of the TWEET command the creator used to
     *                    create the tweet.
     * @param msgHashtags a set containing every hashtags of the tweet; an hashtag
     *                    is a word starting with the '#' character with length > 1.
     * @param writer      the BufferedWriter used by the server to communicate with
     *                    the tweet creator.
     */
    private void transferTweet(String tweet, Set<String> msgHashtags, BufferedWriter writer) {
        Set<BufferedWriter> alreadyReceived = new HashSet<>();
        msgHashtags.forEach(h -> {
            synchronized (crntSubscriptions) {
                if (crntSubscriptions.containsKey(h)) {
                    Set<BufferedWriter> set = crntSubscriptions.get(h);
                    if (set != null) {
                        // Not transmitting tweet to its creator or to client who already received it
                        set.stream().filter(wr -> wr != writer && !alreadyReceived.contains(wr)).forEach(wr -> {
                            try {
                                wr.write(tweet);
                                wr.newLine();
                                wr.flush();
                                alreadyReceived.add(wr);
                            } catch (IOException e) {
                                System.out.println(e);
                            }
                        });
                    }
                }
            }
        });
    }

    // Add client into hashtag's list of subscribers. A client can subscribe to
    // multiple Hashtags in one command.
    /**
     * Add a client's BufferedWriter to the set of subscribers of every hashtag in
     * msgHashtags.
     * 
     * @param msgHashtags set containing the hashtags to which the client wants to
     *                    subscribe
     * @param writer      writer used to communicate with the client
     */
    private void addSubscription(Set<String> msgHashtags, BufferedWriter writer) {
        msgHashtags.forEach(h -> {
            synchronized (crntSubscriptions) {
                if (crntSubscriptions.containsKey(h)) {
                    crntSubscriptions.get(h).add(writer);
                } else {
                    crntSubscriptions.put(h, new HashSet<>(Arrays.asList(writer)));
                }
            }
        });
    }

    /**
     * This method synchronizes calls to System.out.println.
     * 
     * @param msg the msg to be printed.
     */
    private void syncPrintln(String msg) {
        synchronized (System.out) {
            System.out.println(msg);
        }
    }

    /**
     * This main method create a ServerSocket to the port passed as an argument,
     * then start a bunch of threads (the number of thread is defined by the
     * constant NB_THREADS) running the TwitterServer's run method.
     * 
     * @param args the first argument MUST be the port on which the server should
     *             listen. Other aguments will be ignored.
     */
    public static void main(String[] args) {
        String usage = "Usage: ./server <LISTEN_PORT>" + " where <LISTEN_PORT> is an integer";
        if (args.length < 1)
            System.err.println(usage);
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            System.err.println(usage);
            System.exit(1);
        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            TwitterServer ts = new TwitterServer(serverSocket);
            Thread[] threads = new Thread[NB_THREADS];
            for (int i = 0; i < NB_THREADS; i++) {
                threads[i] = new Thread(ts);
                threads[i].start();
            }
            for (Thread t : threads)
                t.join();

            serverSocket.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
