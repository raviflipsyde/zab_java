package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import servers.NodeServerProperties1;

//import servers.NodeServer;


public class UdpServer1 implements Runnable{
	private static final Logger LOG = LogManager.getLogger(UdpServer1.class);
	private static final String HELLO = "U OK?";
	private static final String REPLY = "I M OK";
	private NodeServerProperties1 properties;
	private int port;
	private HashMap<String, Long> heartBeatMap;
	
	public UdpServer1(NodeServerProperties1 nodeProperties){
		this.properties = nodeProperties;
		heartBeatMap = new HashMap<String, Long>();
		
//		Timer t = new Timer();
//		t.schedule(new TimerTask() {
//		    @Override
//		    public void run() {
//		    	long currentTime = System.currentTimeMillis();
//		    	for(Entry<String, Long> entry:heartBeatMap.entrySet()){
//		    		String key = entry.getKey();
//					long lastTimeEntry = entry.getValue();
//					if(currentTime - lastTimeEntry > 10000){
//						String addr[] = key.split(":");
//						String dedadhost = addr[0].trim();
//						int deadPort = Integer.parseInt(addr[1]); 
//						InetSocketAddress socketAddr = new InetSocketAddress(dedadhost, deadPort);
//						LOG.info("Removing "+socketAddr.toString()+" from memberlist");
//						properties.getMemberList().remove(socketAddr);
//					}
//				}
//		    }
//		}, 0, 20000);
		
	} 
	
	public UdpServer1(int port){
		this.port = port;
	} 
	
	public void run() {
		LOG.info("--------------STARTING UDP SERVER--------------"+ properties.getMemberList().size());
		DatagramSocket serverSocket = null;
		try {
			
			serverSocket = new DatagramSocket(properties.getNodePort()+123);
		
			byte[] receiveData = new byte[100];
			byte[] sendData = new byte[100];

			while(true){
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String sentence = new String( receivePacket.getData());
				sentence = sentence.trim();
				
				InetAddress IPAddress = receivePacket.getAddress();
				int port = receivePacket.getPort();
//				InetSocketAddress recvAddress = new InetSocketAddress(IPAddress, port);
				
				long currentTime = System.currentTimeMillis();
//				LOG.info("RECEIVED: " + sentence
//						+ " from "+ recvAddress.toString() 
//						+" at "+ currentTime);
			
				
				String returnSentence = sentence.toUpperCase();
				sendData = returnSentence.getBytes();
				DatagramPacket sendPacket =
						new DatagramPacket(sendData, sendData.length, IPAddress, port);
				serverSocket.send(sendPacket);
				
				String[] addr = sentence.split("::");
				String fulladdr1 = addr[1];
				heartBeatMap.put(fulladdr1, currentTime);
				

			}


		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			serverSocket.close();
		}

	}


}
