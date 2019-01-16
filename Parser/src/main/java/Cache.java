import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.wisc.cs.sdn.simpledns.packet.DNSRdata;
import edu.wisc.cs.sdn.simpledns.packet.DNSResourceRecord;

/**
 * This class holds a cache and index of the cache ordered by TTL. If the cache
 * is full and a new entry should be put in, it removes the entry that will be
 * staled the soonest.
 */
public class Cache {
    private long maxEntries; // Limit on cache size
    private Map<String, CacheEntry> cache; // Map holding cached DNS queries
    private TreeMap<Long, String> cacheIdx; // Index on cache horder by TTL of the entries
    private String name; // A name for the cache, default is empty string.

    public Cache(long maxEntries) {
        this.maxEntries = maxEntries;
        cache = new HashMap<>();
        cacheIdx = new TreeMap<>();
        name = "";
    }

    public Cache(String name, long maxEntries) {
        this(maxEntries);
        this.name = name;
    }

    public void putEntry(String key, DNSResourceRecord record) {
        putEntry(key, new CacheEntry(record));
    }

    /**
     * This method adds an entry to the cache and the index. If the cache is already
     * full, the entry that is closest to being stalled (or the one that has been
     * staled for the longest) is removed before the new one is added.
     * 
     * @param key   the name of a DNS query
     * @param entry CacheEntry containing a DNSResourceRecord holding the answer to
     *              the query
     */
    public void putEntry(String key, CacheEntry entry) {
        long exp = entry.getExpiration();
        if (cache.size() > maxEntries) { // One element must be removed from cache
            // remove entry with shortest TTL, then add new entry
            Entry<Long, String> cacheEntry = cacheIdx.firstEntry();
            cacheIdx.remove(cacheEntry.getKey());
            assert cacheEntry.getKey() < cacheIdx.firstEntry()
                    .getKey() : "Error, replaced value in cache was not the smallest";
            cache.remove(cacheEntry.getValue());
        }
        cache.put(key, entry);
        cacheIdx.put(exp, key); // Idx orders keys by ttl
        // System.out.println("(Cache " + name + ")> Added [" + key + "]->[" + entry.getData() + "]");
    }

    /**
     * This method returns the answer to a DNS query if it is present in the cache
     * and not staled, NULL otherwise.
     * 
     * @param key the name of a DNS query
     * @return DNSRdata contained in the corresponding CacheEntry
     */
    public DNSRdata getEntry(String key) {
        CacheEntry entry = cache.get(key); // NULL if not present
        DNSRdata data = null;
        if (entry != null && entry.isStaled()) { // remove from cache and index
            cache.remove(key);
            cacheIdx.remove(entry.getExpiration());
            entry = null;
        }
        if (entry != null) {
            data = entry.getRecord().getData();
        }
        return data;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String n) {
        this.name = n;
    }
}