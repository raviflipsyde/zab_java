package servers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import serverHandlers.TimeClientHandler;
import util.TcpClient1;

public class NodeServer implements Runnable{

	private static final Logger LOG = LogManager.getLogger(NodeServer.class);
	private String bootstrapHost;
	private int bootstrapPort;
	private int nodePort;
	private List<InetSocketAddress> memberList;
	private List<TimeClient> channelList;
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	Bootstrap b;
	private String myIP;
	private NodeServerProperties properties;

	public NodeServer(String bhost, int bport, int nport){
		this.bootstrapHost = bhost;
		this.bootstrapPort = bport;
		this.nodePort = nport;
		this.properties = new NodeServerProperties();
		this.memberList = new ArrayList<InetSocketAddress>();
		final NodeServer this1 = this;
		myIP = getMyIP();
		workerGroup = new NioEventLoopGroup();
		b = new Bootstrap();
		channelList = new ArrayList<TimeClient>();

		b.group(workerGroup); // (2)
		b.channel(NioSocketChannel.class); // (3)
		b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
		b.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new TimeClientHandler(this1));
			}
		});
	}

	public String getMemberListString(){
		StringBuilder strb = new StringBuilder();
		for(InetSocketAddress addr: memberList){
			strb.append(addr.toString()+ ", ");
		}
		return strb.toString();
	}

	public synchronized void addMemberToList(InetSocketAddress member) {
		this.memberList.add(member);
	}

	public synchronized void removeMemberFromList(InetSocketAddress member) {
		this.memberList.add(member);
	}

	public List<InetSocketAddress> getMemberList() {
		return memberList;
	}
	public void setMemberList(List<InetSocketAddress> memberList) {
		this.memberList = memberList;
	}

	public void run() {

		System.out.println("Start Node Server");

		// send the address to bootstrap, get the member list, get the nodeID
		msgBootstrap();

		LOG.info("ID for this node is :"+ properties.getId());

		//Start the NettyServer at the nodeport
		Thread serverThread = new Thread(new NettyServer(nodePort, this));
		serverThread.start();

		informGroupMembers();

//		writeHistory();
		readHistory();

		changePhase();

	}


	private void informGroupMembers() {

		List<InetSocketAddress> unreachablelist = new ArrayList<InetSocketAddress>();


		for(InetSocketAddress member: memberList){
			String ret;
			try {
				ret = new TcpClient1(member.getHostName(), member.getPort()).sendMsg("JOIN_GROUP:"+myIP+":"+nodePort);
				LOG.info("tcp client recieved "+ ret);	
			} catch (IOException e) {
				unreachablelist.add(member);
				e.printStackTrace();
			}

		}

		for(InetSocketAddress member: unreachablelist){
			this.removeMemberFromList(member);
		}


	}

	private void changePhase() {
		/*
		 The logic of changing phases
		 */
		if( properties.getNodestate().equals(NodeServerProperties.State.ELECTION)){
			startLeaderElection();
		}


		//startRecovery();
		//startBroadcast();


	}



	private void startLeaderElection() {
		// TODO same thread or different thread?
		memberList = this.getMemberList();
		int round = 0;
		//		String myVote = "["++"]";

//		for(InetSocketAddress member: memberList){
//
//			TimeClient tc = new TimeClient(b, member.getHostName(), member.getPort(), this);
//			LOG.info("netty channel client sending join to "+ member.toString());
//			tc.writeMsg("JOIN_GROUP:"+myIP+":"+nodePort);
//			channelList.add(tc);
//
//		}
	}

	// Util functions




	public long msgBootstrap(){
		Socket socket;
		long id = 0;
		try {
			socket = new Socket (bootstrapHost, bootstrapPort);

			PrintWriter out = new PrintWriter (socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader (new InputStreamReader(socket.getInputStream ()));
			//set self_ip:port to bootsstrap

			out.println ("set "+ myIP + ":"+nodePort);


			String memberList = in.readLine ();
			String memberId = in.readLine();
			id = Long.parseLong(memberId);
			LOG.info("MemberID received:"+ id);
			//process memberlist

			this.properties.setId(id);

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


	private void writeHistory() {
		String fileName = "CommitedHistory_"+this.nodePort+".txt";
		File fout = new File(fileName);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			for(Message m: this.properties.getMessageList()){
				bw.write(m.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void readHistory() {

		String fileName = "CommitedHistory_"+this.nodePort+".txt";
		String line = null;
		List<Message> msgList = this.properties.getMessageList();
		try {

			FileReader fileReader = new FileReader(fileName );
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while((line = bufferedReader.readLine()) != null) {
				Message m = new Message(line);
				msgList.add(m);
				System.out.println(m);
			} 

			bufferedReader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Message lastMsg = msgList.get(msgList.size()-1);

		this.properties.setCurrentEpoch(lastMsg.getEpoch());
		this.properties.setLastZxId(lastMsg.getTxId());

	}




}
