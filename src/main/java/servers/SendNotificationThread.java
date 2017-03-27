package servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.TcpClient1;

public class SendNotificationThread implements Runnable{
	private static final Logger LOG = LogManager.getLogger(SendNotificationThread.class);
	private InetSocketAddress address;
	private Notification myNotification;
	private NodeServer nodeServer;
	private ConcurrentLinkedQueue<Notification> electionQueue1;

	

	public Queue<Notification> getElectionQueue1() {
		return electionQueue1;
	}



	public void setElectionQueue1(ConcurrentLinkedQueue<Notification> electionQueue1) {
		this.electionQueue1 = electionQueue1;
	}



	public SendNotificationThread(InetSocketAddress address, 
			Notification myNotification) {

		this.address = address;
		this.myNotification = myNotification;

	}



	public void run() {

		LOG.info("Send Notification:"+this.myNotification.toString()+" to "+ this.address);
		
		TcpClient1 client = new TcpClient1(this.address.getHostName(), this.address.getPort());
		try {
			String response = client.sendMsg("NOTIFICATION:"+this.myNotification.toString());
			if(response.equals("ERROR")){
				//do nothing this is error
			}else{
				
				String resp[] = response.split(":");
				Notification responseNotification = new Notification(resp[1]);

				LOG.info("Received Notification:"+responseNotification.toString()+" from "+ this.address);
				boolean retVal;
				
				retVal = electionQueue1.offer(responseNotification);
				
				synchronized (electionQueue1) {
					electionQueue1.notifyAll();
		        }
				
				LOG.info("electionQueue1.offer:"+retVal+"\n"+electionQueue1+"\n");
//				this.PQueue.add(responseNotification);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}
