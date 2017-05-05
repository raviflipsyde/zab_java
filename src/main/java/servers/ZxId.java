/**
 * 
 */
package servers;

/**
 * @author pghanek
 *
 */
public class ZxId implements Comparable<ZxId>{
	private long epoch;
	private long counter;
	
	public ZxId(long epoch, long counter) {
		this.epoch = epoch;
		this.counter = counter;
	}
	public long getEpoch() {
		return epoch;
	}
	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}
	public long getCounter() {
		return counter;
	}
	public void setCounter(long counter) {
		this.counter = counter;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.epoch + ":" + this.counter;
	}
	public int compareTo(ZxId o) {
		if(this.getEpoch() < o.getEpoch())
			return -1;
		else if(this.getEpoch() > o.getEpoch())
			return 1;
		else if(this.getCounter() < o.getCounter())
			return -1;
		else if(this.getCounter() > o.getCounter())
			return 1;
		else return 0;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (counter ^ (counter >>> 32));
		result = prime * result + (int) (epoch ^ (epoch >>> 32));
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
		ZxId other = (ZxId) obj;
		if (counter != other.counter)
			return false;
		if (epoch != other.epoch)
			return false;
		return true;
	}
	
	
	
	
	
}
