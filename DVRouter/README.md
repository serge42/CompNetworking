# My Distance-Vector Routing
## MyRoutingMessage
This class extends RoutingMessage contains a destination and a cost; it it used by routers to inform their neighbors that they can reach a destination with a given cost.

## MyRouter
This class extends Router and maintains a Map which maps the destination addresses known by the server to the cost of reaching each destination. This Map allows the router to updated its forwarding table to the least cost path for each destination.

## Tuple
The tuple class was used to store the cost and the interface of a destination in the Map of the routers. In the end however, this class was not useful as the interface is already recorded in the forwarding table of the routers; the Map could only store the cost of the best path to the destinations.

## Protocol description
*Initialization:*
When a router is started, it sends a MyRoutingMessage containing its address and a cost of 0 to each of its neighbors. This informs the neighbors that it can reach its own address with 0 cost.

When a router receives a MyRoutingMessage, it computes the total cost necessary to reach the address of the message; this total cost is equal to the cost send in the message plus the cost from this router to its neighbor. If the router already know how to reach the destination, it retrieves the cost of the known route; otherwise it assumes an infinite cost (using the constant Double.POSITIVE_INFINITY).

If the cost of the new route is inferior to the prior one, the router updates its forwarding table to take the quicker route and stores the new total cost in a Map which uses the addresses as keys. If the said Map as been updated, it retransmits a MyRoutingMessage to all of its neighbors containing the modifications (address and new cost).

Since every router always retransmit messages when they find a quicker route, they will eventually find the shortest path to every reachable destinations.

## Known bugs
No bugs where found during my final tests.

## Remarks
This one was much easier than the DNS assignment :)