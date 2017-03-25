package servers;

import java.io.Serializable;

public class Message implements Serializable{
	private long txId;
	private long epoch;
	private long data;
	
	public Message(long id, long epoch, long data) {
		this.txId = id;
		this.epoch = epoch;
		this.data = data;
	}
	
	public Message(String msg) {
		String[] strArray = msg.split(",");
		long id= Long.parseLong(strArray[0]);
		long epoch= Long.parseLong(strArray[1]);
		long data= Long.parseLong(strArray[2]);
		
		this.txId = id;
		this.epoch = epoch;
		this.data = data;
		
		
	}
	
	@Override
	public String toString() {
		return txId + "," + epoch + "," + data ;
	}

	public long getTxId() {
		return txId;
	}

	public long getEpoch() {
		return epoch;
	}

	public long getData() {
		return data;
	}

	
	
	
	
	
	
}
