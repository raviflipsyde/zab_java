package servers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;



public class HeartBeatServer {
	
	public static void main(String args[]){
		try {
			DatagramSocket socket = new DatagramSocket();
			byte[] buf = new byte[50];
			buf = new String("Hi From UDP").getBytes();
			InetAddress address = InetAddress.getByName("152.46.18.157");
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 2115);
			System.out.println("sending Hi..");
			socket.send(packet);
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			System.out.println("received:"+packet.toString());
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
