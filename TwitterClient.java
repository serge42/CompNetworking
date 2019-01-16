import java.net.Socket;
import java.net.SocketAddress;

import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;

/**
 * This class implements a client that can connect to a TwitterServer and send
 * strings to it while simultaneously listening for messages coming from the
 * server.
 */
public class TwitterClient {

    private Scanner scan; // Scanner object used to read user inputs
    private Socket socket; // Socket object used to receive and send strings to the server
    // Runnable waiting for messages from the server and printing them to standard
    // output.
    private Runnable listener = () -> {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // synchronized (System.out) { // Uncessary since sender doesn't print
                System.out.println(line);
                // }
            }
            quit();
        } catch (Exception e) {
            System.err.println(e);
        }
    };

    /**
     * The TwitterClient constructor which initializes the Scanner and saves an
     * instance of Socket.
     * 
     * @param s an instance of Socket that is connected to a TwitterServer.
     */
    public TwitterClient(Socket s) {
        this.scan = new Scanner(System.in);
        this.socket = s;
    }

    /**
     * This method reads user's input from standard input one line at a time and
     * then sends the line to the TwitterServer.
     */
    public void sender() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            while (true) {
                String cmd = scan.nextLine();
                writer.write(cmd);
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * This method quit the TwitterClient when the connection with the server has
     * been lost.
     * 
     * @throws IOException throws the IOException that may come from Socket.close().
     */
    private void quit() throws IOException {
        System.err.println("Connection lost with server, exiting.");
        socket.close();
        System.exit(1);
    }

    /**
     * This main method creates a Socket connected to the server's hostname and port
     * passed as arguments. It then creates an instance of TwitterClient and starts
     * its listener in a new thread.
     * 
     * @param args first argument must be the server hostname or IP, second argument
     *             must be the server port, additional arguments are ignored.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: ./client <SERVER_HOST> <SERVER_PORT>");
            System.exit(1);
        }

        String serverHostname = args[0];
        try {
            int serverPort = Integer.parseInt(args[1]);

            Socket s = new Socket(InetAddress.getByName(serverHostname), serverPort);
            TwitterClient tc = new TwitterClient(s);
            Thread tl = new Thread(tc.listener);
            tl.start();
            tc.sender();

            tl.join();
            s.close();
        } catch (NumberFormatException nfe) {
            System.err.println("SERVER_PORT must be an integer");
            System.exit(1);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

}
