import edu.wisc.cs.sdn.simpledns.packet.*;

/**
 * This class holds a DNSResourceRecord and computes the expiration time of
 * entries based on their TTL.
 */
public class CacheEntry {
    private DNSResourceRecord record;
    private long expiration;

    /**
     * Constructor, computes the expiration based on the current time and the
     * TTL.
     * 
     * @param record
     */
    public CacheEntry(DNSResourceRecord record) {
        this.record = record;
        expiration = (System.currentTimeMillis() / 1000) + record.getTtl();
    }

    /**
     * Creates a new CacheEntry from a DNSResourceRecord.
     * @param record DNSResourceRecord
     * @return a new CacheEntry
     */
    public static CacheEntry multiCreate(DNSResourceRecord record) {
        return new CacheEntry(record);
    }

    /**
     * This method checks if a CacheEntry is staled.
     * @return True if the TTL of the entry has expired, false otherwise.
     */
    public boolean isStaled() {
        long currentSecs = System.currentTimeMillis() / 1000;
        return expiration < currentSecs;
    }

    public long getTtl() {
        return record.getTtl();
    }

    public DNSRdata getData() {
        return record.getData();
    }

    public long getExpiration() {
        return expiration;
    }

    public DNSResourceRecord getRecord() {
        return record;
    }

    public void setRecord(DNSResourceRecord record) {
        this.record = record;
    }
}