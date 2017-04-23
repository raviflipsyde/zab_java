package servers;

/**
 * Created by falak on 4/21/17.
 */
public class Proposal {
    ZxId z;
    String key;
    String value;

    public Proposal(ZxId z, String key, String value) {
        this.z = z;
        this.key = key;
        this.value = value;
    }

    public ZxId getZ() {
        return z;
    }

    public void setZ(ZxId z) {
        this.z = z;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.z.getEpoch() + ":" + this.z.getCounter() + ":" + this.key + ":" + this.value;
    }
}
