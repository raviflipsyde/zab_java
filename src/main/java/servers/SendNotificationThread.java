package servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.TcpClient1;

public class SendNotificationThread implements Runnable{
	private static final Logger LOG = LogManager.getLogger(SendNotificationThread.class);
	private InetSocketAddress address;
	private Notification myNotification;
	private Queue<Notification> PQueue;



	public SendNotificationThread(InetSocketAddress address, 
			Notification myNotification, 
			Queue<Notification> pQueue) {

		this.address = address;
		this.myNotification = myNotification;
		this.PQueue = pQueue;
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

				this.PQueue.add(responseNotification);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}
