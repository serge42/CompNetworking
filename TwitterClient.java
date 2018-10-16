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
 * TODO
 */
public class TwitterClient {

    private Scanner scan; // TODO
    private Socket socket; //TODO
    // TODO
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
     * TODO
     * @param s
     */
    public TwitterClient(Socket s) {
        this.scan = new Scanner(System.in);
        this.socket = s;
    }

    // Scan user input line, sends it to the server then wait for next input.
    // Could also be a runnable but it's unecessary.
    /**
     * TODO
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
     * TODO
     * 
     * @throws IOException
     */
    private void quit() throws IOException {
        System.err.println("Connection lost with server, exiting.");
        socket.close();
        System.exit(1);
    }

    /**
     * TODO
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: ./client <SERVER_HOST> <SERVER_PORT>");
            System.exit(1);
        }

        String serverHostname = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try {
            Socket s = new Socket(InetAddress.getByName(serverHostname), serverPort);
            TwitterClient tc = new TwitterClient(s);
            Thread tl = new Thread(tc.listener);
            tl.start();
            tc.sender();

            tl.join();
            s.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

}
