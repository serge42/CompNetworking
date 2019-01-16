# Sébastien Bouquete


## Definitions
* HASHTAG: a "hashtag" is a word (= a sequence of characters not containing a space) starting with the character '#' and with a length strictly superior to 1.
* <NAME> COMMAND: a "command" is a one line string starting with the name of the command in uppercase characters followed by (at least) one space character, followd by the argument(s) of the "command".  
E.g: "TWEET #Bonjour \n" is a "TWEET command" with one argument: "#Bonjour ".

## Protocol Description
1. Clients make a connection request to the server.
2. If the server has a unoccupied thread, it accepts the connection otherwise the client waits.
3. Clients can send a SUBSCRIBE command to the server followd by one or multiple "hashtags". If the SUBSCRIBE command contains non hashtag words, they are ignored.
4. Clients can send a UNSUBSCRIBE command to the server followed by one or multiple "hashtags". If had previously subscribed to the given "hashtags" their subscriptions will be removed. If they were not subscribed to the given "hashtags" nothing will happen; same holds for non hashtag words passed with the UNSUBSCRIBE command.
5. Clients can send a TWEET command to the server followed by a one line string message. The string message is automatically written to the server's standard output. If the message contains "hashtags", it will be transmitted by the server to every client who previously subscribed to at least one of the contained "hashtags". The server never transmits a message more than once to a given client and it (the server) does not transmit a message to the client that created it.
6. When a client disconnect from the server, its subscriptions are removed.
7. If the server closes the connection while clients are connected, the clients will print an error message and exit.

## Limitations
* No sessions are saved for the clienst; when a client disconnects, all its subscribtions are deleted.
* The number of threads for the server is a fixed value which means the server has to be recompiled to modify the number of threads.
