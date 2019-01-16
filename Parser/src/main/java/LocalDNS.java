import edu.wisc.cs.sdn.simpledns.packet.*;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class holds the internal logic of the LocalDNS
 */
public class LocalDNS {

    public static final int MAX_CACHE_SIZE = 10; // Maximum number of entries in caches

    private DatagramSocket socket;
    private SocketAddress refServer; // Address of a reference DNS server
    private byte[] receiveData; // Used to receive UDP packets
    private DatagramPacket receivePacket; // Used to receive UDP packets
    // Local caches
    private Cache ACache;
    private Cache NSCache;
    private Cache CNAMECache;
    // private Map<Short, SocketAddress> pending;

    public LocalDNS(DatagramSocket socket, SocketAddress server) {
        this.socket = socket;
        this.refServer = server;
        receiveData = new byte[1024];
        receivePacket = new DatagramPacket(receiveData, receiveData.length, refServer);
        ACache = new Cache("A", MAX_CACHE_SIZE);
        NSCache = new Cache("NS", MAX_CACHE_SIZE);
        CNAMECache = new Cache("CNAME", MAX_CACHE_SIZE);
    }

    /**
     * This methods waits for UDP packets from the client holding DNS requests. When
     * a query is received it calls other methods to solve it depending of the type
     * of the DNS query(s).
     * 
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public void start() throws IOException, CloneNotSupportedException {
        while (true) {
            // Waiting for a connection! from a client
            socket.receive(receivePacket);
            // Parsing the packet!
            DNS dns = DNS.deserialize(receivePacket.getData(), receivePacket.getLength());
            SocketAddress clientAdr = receivePacket.getSocketAddress();
            // Is it a query (request)
            if (dns.isQuery()) {
                // System.out.println("A new DNS query:");
                DNS ans = new DNS();
                ans.setId(dns.getId());
                // ans.setRecursionDesired(true);
                ans.setQuery(false);
                ans.setQuestions(dns.getQuestions());
                for (DNSQuestion q : dns.getQuestions()) {
                    if (q.getType() == DNS.TYPE_A) {
                        solveAQuery(q, dns, ans);
                    } else if (q.getType() == DNS.TYPE_CNAME) {
                        // DNSRdata data = CNAMECache.getEntry(q.getName());
                        // solveQuery(q, dns, ans, data, null);
                        solveCNAMEQuery(q, dns, ans);
                    } else if (q.getType() == DNS.TYPE_NS) {
                        // For some reason the given Parser DNS does not work for: www.fb.com NS
                        DNSRdata data = NSCache.getEntry(q.getName());
                        List<DNSResourceRecord> cacheList = new ArrayList<>();
                        if (data != null)
                            cacheList.add(new DNSResourceRecord(q.getName(), q.getType(), data));
                        solveQuery(q, dns, ans, cacheList);
                    }
                }
                send(ans.serialize(), clientAdr);
            } else {
                // System.out.println("A DNS reply !");
                // checkReply(dns, receivePacket);
            }
        }
    }

    /**
     * The methods that solves A queries. It first checks if it holds the answer in
     * the ACache. Then it checks the CNAME cache recursively in the hope of finding
     * an entry that is also present in the ACache. It then calls the method
     * solveQuery to decide if a query to the reference server is necessary.
     * 
     * @param q     DNSQuestion trying to get solved
     * @param query DNS contains q and possibly other queries (DNSQuestion)
     * @param ans   instance of DNS used to return the answer of the query to the
     *              client
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    private void solveAQuery(DNSQuestion q, DNS query, DNS ans) throws IOException, CloneNotSupportedException {
        DNSRdata data = ACache.getEntry(q.getName());
        DNSRdata inCNAME = CNAMECache.getEntry(q.getName());
        DNSRdata inCNAMENew = null;
        List<DNSResourceRecord> possibleAns = null;
        if (inCNAME != null && data == null) {
            possibleAns = new ArrayList<>();
            possibleAns.add(new DNSResourceRecord(q.getName(), DNS.TYPE_CNAME, inCNAME));
            data = ACache.getEntry(inCNAME.toString());
        }

        // Maybe it is in CNAMECache -> KEEP TRACK OF CNAMES TOO
        while (data == null && inCNAME != null) {
            inCNAMENew = CNAMECache.getEntry(inCNAME.toString());
            if (inCNAMENew != null)
                possibleAns.add(new DNSResourceRecord(inCNAME.toString(), DNS.TYPE_CNAME, inCNAMENew));
            inCNAME = inCNAMENew;
            if (inCNAME != null) // Can happen if inCNAMENew is staled
                data = ACache.getEntry(inCNAME.toString());
        }
        if (data != null)
            possibleAns.add(new DNSResourceRecord(q.getName(), q.getType(), data));
        else
            possibleAns = null;
        solveQuery(q, query, ans, possibleAns);
    }

    /**
     * Check the CNAMECache then called solveQuery with the data it found (NULL if
     * no data was found).
     * 
     * @param q     DNSQuestion trying to get solved
     * @param query DNS contains q and possibly other queries (DNSQuestion)
     * @param ans   instance of DNS used to return the answer of the query to the
     *              client
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    private void solveCNAMEQuery(DNSQuestion q, DNS query, DNS ans) throws IOException, CloneNotSupportedException {
        List<DNSRdata> entries = checkCNAMECache(q);
        // List<DNSRdata> entries = new ArrayList<>();
        if (entries.size() > 0) {
            List<DNSResourceRecord> records = new ArrayList<>();
            entries.stream().filter(d -> d != null)
                    .forEach(d -> records.add(new DNSResourceRecord(d.toString(), DNS.TYPE_CNAME, d)));
            solveQuery(q, query, ans, records);
        } else {
            solveQuery(q, query, ans, null);
        }
    }

    /**
     * Checks the CNAMECache recursively until no more entries can be found.
     * 
     * @param q DNSQuestion containing the name of the query.
     * @return A List of DNSRdata containing the entries found in the CNAMECache.
     */
    private List<DNSRdata> checkCNAMECache(DNSQuestion q) {
        List<DNSRdata> entries = new ArrayList<>();
        DNSRdata record = CNAMECache.getEntry(q.getName());
        entries.add(record);
        while (record != null) {
            record = CNAMECache.getEntry(record.toString());
            entries.add(record);
        }
        return entries;
    }

    /**
     * This method decides if a reply can be returned to the client based on the
     * data found in the cache(s) or if a query has to be made to reference server
     * 
     * @param q           DNSQuestion
     * @param query       DNS contains the query q
     * @param ans         DNS to return the reply
     * @param possibleAns Data found in Cache(s); may be NULL
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    private void solveQuery(DNSQuestion q, DNS query, DNS ans, List<DNSResourceRecord> possibleAns)
            throws IOException, CloneNotSupportedException {
        if (possibleAns != null && possibleAns.size() > 0) {
            if (possibleAns != null)
                ans.addAllAnswers(possibleAns);
            // ans.addAnswer(new DNSResourceRecord(q.getName(), q.getType(), data));
            System.out.println("Answered from cache");
        } else {
            DNS reply = getAnswer(q, query);
            // System.out.println(reply);
            ans.addAllAnswers(reply.getAnswers());
            ans.setAuthorities(reply.getAuthorities());
            ans.setAdditional(reply.getAuthorities());
        }
    }

    /**
     * This method solves DNS queries by asking the reference server. The queries
     * are handled differently following their types. This method also puts data in
     * the caches when it receives replies from the reference server.
     * 
     * @param question DNSQuestion
     * @param query    DNS query
     * @return DNS containing the reply for the client
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    private DNS getAnswer(DNSQuestion question, DNS query) throws IOException, CloneNotSupportedException {
        DNS ans = askServer(question, query, refServer);
        List<DNSResourceRecord> answers = ans.getAnswers();
        // System.out.println("ANSWERS: " + ans);
        if (question.getType() == DNS.TYPE_A) {
            List<DNSResourceRecord> AReplies = answers.stream().filter(r -> r.getType() == DNS.TYPE_A)
                    .collect(Collectors.toList());
            List<DNSResourceRecord> CNAMEReplies = answers.stream().filter(r -> r.getType() == DNS.TYPE_CNAME)
                    .collect(Collectors.toList());
            if (AReplies.size() > 0) {
                AReplies.forEach(record -> ACache.putEntry(record.getName(), record));
                CNAMEReplies.forEach(record -> CNAMECache.putEntry(record.getName(), record));
            } else if (CNAMEReplies.size() > 0) {
                // resolve CNAME query(s) until an A reply comes back -> never happens if
                // ref-server solves recursively
                assert false : "This should not happen";
            } else if (ans.getAnswers().size() <= 0 && !(ans.isRecursionAvailable() && ans.isRecursionDesired())) {
                // Solve iterative query
                // System.out.println(ans);
                ans = iterativeQuery(question, query, ans);
                // Now we got an answer but it may still be in CNAME instead of A
                AReplies = ans.getAnswers().stream().filter(r -> r.getType() == DNS.TYPE_A)
                        .collect(Collectors.toList());
                CNAMEReplies = ans.getAnswers().stream().filter(r -> r.getType() == DNS.TYPE_CNAME)
                        .collect(Collectors.toList());
                if (AReplies.size() <= 0 && CNAMEReplies.size() > 0) {
                    // Add answer to cache
                    DNSResourceRecord cname = CNAMEReplies.get(0);
                    CNAMECache.putEntry(question.getName(), new CacheEntry(cname));

                    DNSQuestion q = new DNSQuestion(cname.getData().toString(), DNS.TYPE_A);
                    DNS newQuery = (DNS) query.clone();
                    newQuery.addQuestion(q);
                    DNS newAns = new DNS();
                    solveAQuery(q, query, newAns); // solveAQuery checks cache berfore answering

                    AReplies = newAns.getAnswers().stream().filter(r -> r.getType() == DNS.TYPE_A)
                            .collect(Collectors.toList());
                    CNAMEReplies = newAns.getAnswers().stream().filter(r -> r.getType() == DNS.TYPE_CNAME)
                            .collect(Collectors.toList());
                    if (CNAMEReplies.size() > 0) {
                        ans.addAllAnswers(CNAMEReplies);
                    }
                    if (AReplies.size() > 0) {
                        ans.addAllAnswers(AReplies);
                    }
                } else if (AReplies.size() > 0) {
                    AReplies.forEach(record -> ACache.putEntry(record.getName(), record));
                }
            }
            // return ans;
        } else if (question.getType() == DNS.TYPE_CNAME) {
            List<DNSResourceRecord> CNAMEReplies = answers.stream().filter(r -> r.getType() == DNS.TYPE_CNAME)
                    .collect(Collectors.toList());
            CNAMEReplies.forEach(record -> CNAMECache.putEntry(record.getName(), record));
            if (CNAMEReplies.size() <= 0 && !(ans.isRecursionAvailable() && query.isRecursionDesired())) {
                ans = iterativeQuery(question, query, ans);
                while (ans.getAnswers().size() > 0) {
                    CNAMEReplies = ans.getAnswers().stream().filter(r -> r.getType() == DNS.TYPE_CNAME)
                            .collect(Collectors.toList());
                    CNAMEReplies.forEach(record -> CNAMECache.putEntry(record.getName(), record));
                    DNS newAns = new DNS();
                    DNS newQuery = (DNS) query.clone();
                    DNSQuestion newQ = new DNSQuestion(
                            ans.getAnswers().get(ans.getAnswers().size() - 1).getData().toString(), DNS.TYPE_CNAME);
                    newQuery.addQuestion(newQ);
                    newAns = getAnswer(newQ, newQuery);
                    // System.err.println("should have an ansewr: \n" + newAns);
                    if (newAns.getAnswers().size() <= 0)
                        break;
                    ans.addAllAnswers(newAns.getAnswers());
                }
                // CONTINUE UNTIL ANSWERS is empty SINCE THERE MIGHT BE A CHAIN OF CNAME ANSWERS
            }
            // return ans;
        } else if (question.getType() == DNS.TYPE_NS) {
            // First get CNAME of question
            List<DNSResourceRecord> NSReplies = ans.getAnswers();
            NSReplies.forEach(r -> NSCache.putEntry(r.getName(), r));
            if (NSReplies.size() <= 0 && !(ans.isRecursionAvailable() && ans.isRecursionDesired())) {
                // Iterative query ...
                // Start by making asking for CNAME
                DNS cnameAns = new DNS();
                DNSQuestion cnameQ = new DNSQuestion(question.getName(), DNS.TYPE_CNAME);
                solveCNAMEQuery(cnameQ, query, cnameAns);
                if (cnameAns.getAuthorities().size() > 0) {
                    return cnameAns;
                } else {
                    // TODO ITERATIVE NS QUERIES

                    // List<DNSResourceRecord> cnames = cnameAns.getAnswers();
                    // DNSResourceRecord cname = getLastRecord(cnames);
                    // DNSQuestion newQ = new DNSQuestion(cname.getData().toString(), DNS.TYPE_NS);
                    // DNS newQuery = (DNS) query.clone();
                    // newQuery.addQuestion(newQ);
                    // ans = iterativeQuery(newQ, newQuery, ans);
                }
            }
            // return ans;
        }
        return ans;
    }

    /**
     * This method solves an iterative DNS query.
     * 
     * @param question DNSQuestion
     * @param query    DNS query
     * @param ans      DNS containing the reply for the client
     * @return
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    private DNS iterativeQuery(DNSQuestion question, DNS query, DNS ans)
            throws IOException, CloneNotSupportedException {
        try {
            // System.err.println("ITERATIVE: q=" + question.getName());
            while (ans.getAnswers().size() <= 0) {
                DNSResourceRecord authority = ans.getAuthorities().get(0);
                // Make sure we get an IPv4
                DNSResourceRecord additional = ans.getAdditional().stream().filter(rec -> rec.getType() == DNS.TYPE_A)
                        .collect(Collectors.toList()).get(0);
                NSCache.putEntry(authority.getName(), authority);
                SocketAddress address = new InetSocketAddress(additional.getData().toString(), 53);
                // System.err.println(authority.toString() + " || " + additional.toString());
                ans = askServer(question, query, address);
            }
            return ans;
        } catch (IndexOutOfBoundsException e) {
            // TODO: handle exception
            // System.err.println("No authority or additional returned, no answer found");
            return ans;
        }
    }

    /**
     * This method creates a DNS query for the server, waits for the reply and
     * returns an instance of DNS containing the reply.
     * 
     * @param question
     * @param initialQuery
     * @param server
     * @return
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    private DNS askServer(DNSQuestion question, DNS initialQuery, SocketAddress server)
            throws IOException, CloneNotSupportedException {
        DNS dns = (DNS) initialQuery.clone();
        dns.addQuestion(question);
        byte[] data = dns.serialize();
        send(data, server);
        socket.receive(receivePacket);
        DNS ans = DNS.deserialize(receivePacket.getData(), receivePacket.getLength());
        // System.out.println("Reply from:" + receivePacket.getSocketAddress());
        // System.out.println(ans);
        return ans;
    }

    /**
     * This methods sends a DatagramPacket to the given SocketAddress.
     * @param data The data to be transmitted
     * @param addr The SocketAddress of the recipient
     */
    private void send(byte[] data, SocketAddress addr) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, addr);
            // System.out.println("Sending to" + packet.getSocketAddress());
            socket.send(packet);
        } catch (Exception e) {
            // TODO: handle exception
            // System.err.println("Sending to " + addr + " failed");
        }
    }

    // UNUSED
    private DNSResourceRecord getLastRecord(List<DNSResourceRecord> list) {
        return list.stream().filter(r -> r != null).reduce((a, b) -> b).get();
    }

    public static void main(String[] args) {
        // A buffer to be filled by the socket data
        SocketAddress server = null;
        DatagramSocket socket = null;
        if (args.length < 2) {
            System.err.println("Usage: LocalDNS <port> <ref-server>");
            System.exit(1);
        }
        try {
            int port = Integer.parseInt(args[0]);
            // server = Inet4Address.getByName(args[1]);
            server = new InetSocketAddress(args[1], 53);
            socket = new DatagramSocket(port);
        } catch (Exception e) {
            System.err.println("Wrong parameter types");
        }

        try {
            new LocalDNS(socket, server).start();
        } catch (IOException ex) {
            if (socket != null)
                socket.close();
            System.err.println("IOException occured" + ex.getMessage());
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
