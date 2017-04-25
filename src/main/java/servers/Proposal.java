package servers;

public class Proposal implements Comparable<Proposal> {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((z == null) ? 0 : z.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Proposal other = (Proposal) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (z == null) {
			if (other.z != null)
				return false;
		} else if (!z.equals(other.z))
			return false;
		return true;
	}

	public int compareTo(Proposal o) {
		return this.z.compareTo(o.z);
		
	}
}
