package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import servers.NodeServer;

public class UdpClient implements Runnable{
	private static final String HELLO = "U OK?";
	private static final String REPLY = "I M OK";
	private NodeServer nodeServer;
	static List<InetSocketAddress>  memberList = new ArrayList<InetSocketAddress>();
	
	public UdpClient(NodeServer ns){
		this.nodeServer = ns;
		
	}
	
	
	public void run() {

		while(true){
			try {
				byte[] receiveData = new byte[100];
				byte[] sendData = new byte[100];
				
				for(InetSocketAddress addr: memberList){
					DatagramSocket clientSocket = new DatagramSocket();
					
					InetAddress IPAddress = InetAddress.getByName(addr.getHostName());
					int port = addr.getPort()+123;
					sendData = HELLO.getBytes();
				
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					
					System.out.println("Sending to:" + addr.toString());
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
