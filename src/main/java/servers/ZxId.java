/**
 * 
 */
package servers;

/**
 * @author Prathamesh
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
		return "ZxID [ Epoch = " + this.epoch + ",Counter = " + this.counter + "]";
	}
	public int compareTo(ZxId o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
}
