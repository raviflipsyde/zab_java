package servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.TcpClient1;

public class NodeServer implements Runnable{

	private static final Logger LOG = LogManager.getLogger(NodeServer.class);
	private String bootstrapHost;
	private int bootstrapPort;
	private int nodePort;
	private List<InetSocketAddress> memberList;
	private String myIP;

	private long id;
	private long lastEpoch;
	private long currentEpoch;
	private long lastZxId;
	
	
	
	public long getId() {
		return id;
	}



	public void setId(long id) {
		this.id = id;
	}



	public NodeServer(String bhost, int bport, int nport){
		this.bootstrapHost = bhost;
		this.bootstrapPort = bport;
		this.nodePort = nport;
		this.memberList = new ArrayList<InetSocketAddress>();
		myIP = getMyIP();
	}

	
	
public String getMemberList(){
	StringBuilder strb = new StringBuilder();
	for(InetSocketAddress addr: memberList){
		strb.append(addr.toString()+ ", ");
	}
	return strb.toString();
}
	
	
	public synchronized void addMemberToList(InetSocketAddress member) {
		this.memberList.add(member);
	}



	public void run() {

		System.out.println("in Node Server run");

		// send the address to bootstrap
		long id = msgBootstrap();
		this.setId(id);
		LOG.info("ID for this node is :"+ id);
		// get the memberlist
		// memberlist set after msgBootstrap call

		try {
			// Start the tcp serve to listen to incoming msgs
			//			Thread serverThread = new Thread(new TcpServer(nodePort));
			for(InetSocketAddress member: memberList){
				
				String	ret  = new TcpClient1(member.getHostName(), member.getPort()).sendMsg("JOIN_GROUP:"+myIP+":"+nodePort);
				LOG.info("tcp client recieved "+ ret);	
		

			}

			Thread serverThread = new Thread(new NettyServer(nodePort, this));
			serverThread.start();

			
//			this.runServer(nodePort);
			// state change methods.
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// connect with all the members
		// start phase 0



	}


	public long msgBootstrap(){
		Socket socket;
		try {
			socket = new Socket (bootstrapHost, bootstrapPort);

			PrintWriter out = new PrintWriter (socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader (new InputStreamReader(socket.getInputStream ()));
			//set self_ip:port to bootsstrap

			out.println ("set "+ myIP + ":"+nodePort);


			String memberList = in.readLine ();
			String memberId = in.readLine();
			Long id = Long.parseLong(memberId);
			
			//process memberlist
			parseMemberList(memberList);
			
			out.close ();
			in.close();
			socket.close ();


		} catch (UnknownHostException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return id;
	}




	private void parseMemberList(String memberList) {

		String[] list = memberList.split(",");
		System.out.println("Members");
		for(String s:list){
			String[] address = s.split(":");
			String ip = address[0];
			int port = Integer.parseInt(address[1]);
			if(myIP.equals(ip) && nodePort== port){}
			else{
				InetSocketAddress addr = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
				this.memberList.add(addr);
			}
			
		}

	}


	public String getMyIP(){
		BufferedReader in = null;
		String ip = " ";
		try {
			URL whatismyip = new URL("http://ipv4bot.whatismyipaddress.com/");
			in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			ip = in.readLine();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ip;
	}
	
	
	

}
