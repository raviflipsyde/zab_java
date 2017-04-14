package servers;

import java.io.Serializable;

public class Message implements Serializable{
	private ZxId zxid;
	private long data;
	
	public Message(ZxId zxid, long data) {
		this.zxid = zxid;;
		this.data = data;
	}
	
	public Message(String msg) {
		String[] strArray = msg.split(",");
		long epoch = Long.parseLong(strArray[0]);
		long counter = Long.parseLong(strArray[1]);
		long data = Long.parseLong(strArray[2]);
		
		this.zxid = new ZxId(epoch,counter);
		this.data = data;
		
		
	}
	
	@Override
	public String toString() {
		return  zxid.getEpoch() + "," + zxid.getCounter() + "," + data;
	}

	public ZxId getZxid() {
		return zxid;
	}

	public long getData() {
		return data;
	}

	
	
	
	
	
	
}
