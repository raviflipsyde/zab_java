package servers;

public class Vote implements Comparable<Vote>{

	private long TxId;
	private long epoch;
	private long id;
	
	
	public long getTxId() {
		return TxId;
	}


	public void setTxId(long txId) {
		TxId = txId;
	}


	public long getEpoch() {
		return epoch;
	}


	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}


	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}

	

	public Vote(long txId, long epoch, long id) {
		
		TxId = txId;
		this.epoch = epoch;
		this.id = id;
	}


	public int compareTo(Vote o) {
		if(this.epoch<o.epoch)
			return -1;
		else if(this.epoch>o.epoch)
			return 1;
		else if(this.TxId<o.TxId)
			return -1;
		else if(this.TxId>o.TxId)
			return 1;
		else if(this.id<o.id)
			return -1;
		else if(this.id>o.id)
			return -1;
		else return 0; // shoudn't happen
		
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (TxId ^ (TxId >>> 32));
		result = prime * result + (int) (epoch ^ (epoch >>> 32));
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
		if (TxId != other.TxId)
			return false;
		if (epoch != other.epoch)
			return false;
		if (id != other.id)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Vote [TxId=" + TxId + ", epoch=" + epoch + ", id=" + id + "]";
	}
	
	
	
}
