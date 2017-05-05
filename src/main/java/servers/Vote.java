package servers;

/**
 * @author rpatel16, pghanek
 *
 */
public class Vote implements Comparable<Vote>{

	private ZxId zxid;
	private long id;
	
	public ZxId getZxid() {
		return zxid;
		 	}

	public void setZxid(ZxId zxid) {
		this.zxid = zxid;
	}
	

	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public Vote(ZxId zxid, long id) {	
		this.zxid = zxid;
 		this.id = id;
	}


	public int compareTo(Vote o) {

		if(this.zxid.getEpoch() < o.zxid.getEpoch())
			return -1;
		else if(this.zxid.getEpoch() > o.zxid.getEpoch())
			return 1;
		else if(this.zxid.getCounter() < o.zxid.getCounter())
			return -1;
		else if(this.zxid.getCounter() > o.zxid.getCounter())
			return 1;
		else if(this.id < o.id)
			return -1;
		else if(this.id > o.id)
			return 1;
		else return 0; // shoudn't happen
		
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (zxid.getCounter() ^ (zxid.getCounter() >>> 32));
		result = prime * result + (int) (zxid.getEpoch() ^ (zxid.getEpoch() >>> 32));
		result = prime * result + (int) (id ^ (id >>> 32));
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
		Vote other = (Vote) obj;
		if (zxid.getCounter() != other.zxid.getCounter())
			return false;
		if (zxid.getEpoch() != other.zxid.getEpoch())
			return false;
		if (id != other.id)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Vote [ Id =" + this.id + ",Epoch = " + this.zxid.getEpoch() + ",Counter = " + this.zxid.getCounter() + "]";
	}
	
	
	
}
