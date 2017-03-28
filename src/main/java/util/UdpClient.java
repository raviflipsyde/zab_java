package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import servers.NodeServer;

public class UdpClient implements Runnable{
	private static final Logger LOG = LogManager.getLogger(UdpClient.class);
	
	private NodeServer nodeServer;
	
	
	public UdpClient(NodeServer ns){
		this.nodeServer = ns;
		
	}
	
	
	public void run() {
		LOG.info("--------------STARTING UDP CLIENT--------------"+ nodeServer.getMemberList().size());
		while(true){
			try {
				byte[] receiveData = new byte[100];
				byte[] sendData = new byte[100];
				
				for(InetSocketAddress addr: nodeServer.getMemberList()){
					DatagramSocket clientSocket = new DatagramSocket();
					
					InetAddress IPAddress = InetAddress.getByName(addr.getHostName());
					int port = addr.getPort()+123;
					String HELLO = nodeServer.getMyIP()+":"+nodeServer.getNodePort() ;
					
					sendData = HELLO.getBytes();
				
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					
					LOG.info("Sending to:" + addr.toString());
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