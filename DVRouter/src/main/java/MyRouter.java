import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import simplenet.*;

public class MyRouter extends Router {

    private double last_send; // only needed for recurrent network updates
    // Maps addresses to an interface and a total cost
    private Map<Integer, Tuple<Integer, Double>> dest_addr;

    public MyRouter() {
        last_send = 0;
        dest_addr = new HashMap<>();
    }

    @Override
    public void initialize() {
        // System.out.println("Router [" + my_address() + "] have " + interfaces() + "
        // neighbors.");
        IntStream.range(0, interfaces())
                .forEach(i -> send_message(new MyRoutingMessage(my_address(), 0), i));
    }

    @Override
    public void process_routing_message(RoutingMessage msg, int ifx) {
        MyRoutingMessage my_msg = (MyRoutingMessage) msg;
        // System.out.println("[" + my_address() + "] got a routing message from interface " + ifx + ", dest addr: "
        //         + ((MyRoutingMessage) msg).address);

        double alt_cost = my_msg.cost + link_cost(ifx);
        double cost = dest_addr.containsKey(my_msg.address) ? dest_addr.get(my_msg.address).y : Double.POSITIVE_INFINITY;

        if (alt_cost < cost) { // update
            dest_addr.put(my_msg.address, new Tuple<>(ifx, alt_cost));
            set_forwarding_entry(my_msg.address, ifx);
            // Transmit update to neighbors
            IntStream.range(0, interfaces()).filter(i -> i != ifx)
                    .forEach(i -> send_message(new MyRoutingMessage(my_msg.address, alt_cost), i));
        }

    }

    public static void main(String[] args) {
        MyRouter r1 = new MyRouter();
        r1.initialize();
    }
}

class MyRoutingMessage extends RoutingMessage {
    public int address;
    public double cost;
    // public message_type type;

    public MyRoutingMessage(int addr, double cost) {
        this.address = addr;
        this.cost = cost;
    }

}