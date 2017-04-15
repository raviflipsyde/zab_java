package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import servers.NodeServerProperties1;

public class UdpClient1 implements Runnable{
	
	private static final Logger LOG = LogManager.getLogger(UdpClient1.class);
	private NodeServerProperties1 properties;
	
	
	public UdpClient1(NodeServerProperties1 nodeProperties){
		this.properties = nodeProperties;
		
	}
	
	
	public void run() {
		LOG.info("--------------STARTING UDP CLIENT--------------:::"+ properties.getMemberList().size());
		while(true){
			try {
				byte[] receiveData = new byte[100];
				byte[] sendData = new byte[100];
				
				for(Entry<Long, InetSocketAddress> addr: properties.getMemberList().entrySet()){
					DatagramSocket clientSocket = new DatagramSocket();
					
					InetAddress IPAddress = InetAddress.getByName(addr.getValue().getHostName());
					int port = addr.getValue().getPort()+123;
					String HELLO = properties.getNodeHost()+":"+properties.getNodePort() ;
					HELLO = HELLO + "::"+ HELLO+ "::"+ HELLO+ "::"+ HELLO;
					sendData = HELLO.getBytes();
				
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					
//					LOG.info("Sending to:" + addr.toString());
					clientSocket.send(sendPacket);
					clientSocket.close();
				}
				
//				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//				clientSocket.receive(receivePacket);
//				String modifiedSentence = new String(receivePacket.getData());
//				System.out.println("FROM SERVER:" + modifiedSentence);
//				
				
			Thread.sleep(4000);

			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		

	}

}
