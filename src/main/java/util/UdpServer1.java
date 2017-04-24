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
		
		Timer t = new Timer();
		t.schedule(new TimerTask() {
		    @Override
		    public void run() {
		    	HashMap<String, Long> removeMap = new HashMap<String, Long>();
		    	
		    	long currentTime = System.currentTimeMillis();
		    	for(Entry<String, Long> entry:heartBeatMap.entrySet()){
		    		String key = entry.getKey();
					long lastTimeEntry = entry.getValue();
					if(currentTime - lastTimeEntry > 10000){
						removeMap.put(key, lastTimeEntry);
//						String addr[] = key.split(":");
//						String dedadhost = addr[0].trim();
//						int deadPort = Integer.parseInt(addr[1]); 
//						InetSocketAddress socketAddr = new InetSocketAddress(dedadhost, deadPort);
//						LOG.debug("Removing "+socketAddr.toString()+" from memberlist");
//						properties.getMemberList().remove(socketAddr);
					}
				}
		    	
		    	for(Entry<String, Long> entry:removeMap.entrySet()){
		    		heartBeatMap.remove(entry.getKey());
		    	}
		    	
		    	if(heartBeatMap.size() < properties.getMemberList().size()/2){
		    		LOG.debug("Throwing runtime exception");
//		    		properties.removeMemberFromList(properties.getLeaderId());
		    		LOG.debug("Removing leader from memberlist");
		    		properties.setLeaderId(0);
		    		properties.setNodestate(NodeServerProperties1.State.ELECTION);
		    		throw new RuntimeException();
		    	}
		    }
		}, 20000, 20000);
		
	} 
	
	public UdpServer1(int port){
		this.port = port;
	} 
	
	public void run() {
		LOG.debug("--------------STARTING UDP SERVER--------------"+ properties.getMemberList().size());
		DatagramSocket serverSocket = null;
		try {
			
			try{
				serverSocket = new DatagramSocket(properties.getNodePort()+123);
			 }
			catch(java.net.BindException e){
				serverSocket.close();
				serverSocket = new DatagramSocket(properties.getNodePort()+123);
			}
			
			
			LOG.debug("ServerSocketState:"+ serverSocket.isBound());
			LOG.debug("ServerSocketState:"+ serverSocket.isClosed());
			LOG.debug("ServerSocketState:"+ serverSocket.isConnected());
			byte[] receiveData = new byte[100];
			byte[] sendData = new byte[100];

			while(true){
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String sentence = new String( receivePacket.getData());
				sentence = sentence.trim();
				
				InetAddress IPAddress = receivePacket.getAddress();
				int port = receivePacket.getPort();
				InetSocketAddress recvAddress = new InetSocketAddress(IPAddress, port);
				
				long currentTime = System.currentTimeMillis();
//				LOG.debug("RECEIVED: " + sentence
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

	public static void main(String args[]){
		
		int port = Integer.parseInt(args[0]);
		new UdpServer1(port).run();
	}
}