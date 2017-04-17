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
		int counter =5;

		while(counter>0){


			byte[] receiveData = new byte[100];
			byte[] sendData = new byte[100];

			DatagramSocket clientSocket;
			
			try {
				clientSocket = new DatagramSocket();

				InetSocketAddress leaderAddr = properties.getLeaderAddress();
				InetAddress IPAddress = leaderAddr.getAddress();
				int port = leaderAddr.getPort()+123;

				String HELLO = properties.getNodeHost()+":"+properties.getNodePort() ;
				HELLO = HELLO + "::"+ HELLO+ "::"+ HELLO+ "::"+ HELLO;
				sendData = HELLO.getBytes();

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

				//			LOG.info("Sending to:" + addr.toString());
				clientSocket.send(sendPacket);
				clientSocket.setSoTimeout(4000);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				counter =5;
				clientSocket.close();
				Thread.sleep(4000);
				
			} catch (SocketException e) {
				e.printStackTrace();
					
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOG.info(e.getMessage());
				counter--;
				continue;	
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				LOG.info(e.getMessage());
			}


		}
		LOG.info("Throwing runtime exception");
		properties.removeMemberFromList(properties.getLeaderId());
		LOG.info("Removing leader from memberlist");
		properties.setLeaderId(0);
		properties.setNodestate(NodeServerProperties1.State.ELECTION);
		throw new RuntimeException();


	}

}
