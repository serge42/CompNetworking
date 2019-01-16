import edu.wisc.cs.sdn.simpledns.packet.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

public class Requests {    
    private DatagramSocket socket;
    private InetAddress address;
 
    private byte[] buf;

    public Requests() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = Inet4Address.getByName("localhost");
    }

    public DatagramPacket sendTest(byte[] msg) throws UnknownHostException, IOException {
        InetAddress addr = Inet4Address.getByName("8.8.8.8");
        DatagramPacket packet = new DatagramPacket(msg, msg.length, addr, 53);
        socket.send(packet);

        buf = new byte[1024];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        socket.receive(recv);
        return recv;
    }

    public static void main(String[] args) {
        Random r = new Random();

        try {
            Requests req = new Requests();
            
            DNS dns = new DNS();
            dns.setRecursionDesired(true);
            dns.setQuery(true);
            dns.addQuestion(new DNSQuestion("research.inf.usi.ch.", DNS.TYPE_A));
            dns.setId((short) r.nextInt());

            DatagramPacket res = req.sendTest(dns.serialize());
            DNS ans = DNS.deserialize(res.getData(), res.getLength());
            System.out.println(ans);
        } catch (Exception e) {
            //TODO: handle exception... Sure I'm totally gonna do that, no kidding
        }
    }
}