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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Queue;

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
import util.UdpClient;
import util.UdpServer;

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
	public static ConcurrentLinkedQueue<Notification> electionQueue123 = new ConcurrentLinkedQueue<Notification>();
	//private long electionRound;
	
	public NodeServer(String bhost, int bport, int nport){
		this.bootstrapHost = bhost;
		this.bootstrapPort = bport;
		this.nodePort = nport;
		this.properties = new NodeServerProperties();
		this.memberList = new CopyOnWriteArrayList<InetSocketAddress>();
		final NodeServer this1 = this;
		myIP = getMyIP();
		if(bhost.equals("localhost"))
			myIP = "localhost";
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
	
	

	public int getNodePort() {
		return nodePort;
	}



	public NodeServerProperties getProperties() {
		return properties;
	}

	

	public void run() {

		System.out.println("Start Node Server");

		// send the address to bootstrap, get the member list, get the nodeID
		msgBootstrap();

		LOG.info("ID for this node is :"+ properties.getId());

		
		Thread udpserverThread = new Thread(new UdpServer(this));
		udpserverThread.start();
		
		//Start the NettyServer at the nodeport
		Thread serverThread = new Thread(new NettyServer(nodePort, this));
		serverThread.start();
		
		Thread udpClientThread = new Thread(new UdpClient(this));
		udpClientThread.start();

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
		long leaderID = properties.getId();
		if( properties.getNodestate().equals(NodeServerProperties.State.ELECTION)){
			LOG.info("Begin Leader Election---------");
			Vote leaderVote = startLeaderElection();
			LOG.info("End Leader Election---------");
			LOG.info("Leader ID:"+leaderVote.getId() );
			if(leaderVote.getId() == properties.getId()){
				properties.setLeader(true);
				properties.setNodestate(NodeServerProperties.State.LEADING);
				leaderID = properties.getId();
			}
			else{
				properties.setLeader(true);
				properties.setNodestate(NodeServerProperties.State.FOLLOWING);
				leaderID = leaderVote.getId();
			}
		}


		//startRecovery();
		//startBroadcast();


	}



	private Vote startLeaderElection() {
		// TODO same thread or different thread?
		memberList = this.getMemberList();
		
		this.properties.setElectionRound(this.properties.getElectionRound()+1);
		
		HashMap<Long, Vote> receivedVote = new HashMap<Long, Vote>();
		HashMap<Long, Long> receivedVotesRound = new HashMap<Long, Long>();
		HashMap<Long, Vote> OutOfElectionVotes = new HashMap<Long, Vote>();
		HashMap<Long, Long> OutOfElectionVotesRound = new HashMap<Long, Long>();
		long limit_timeout = 10000;
		long timeout = 1000;
		
		//Queue<Notification> currentElectionQueue = new ConcurrentLinkedQueue<Notification>();
//		this.getProperties().setElectionQueue(currentElectionQueue);
		ConcurrentLinkedQueue<Notification>  currentElectionQueue = electionQueue123;
		
		Vote myVote123 = new Vote(this.properties.getLastZxId(), this.properties.getCurrentEpoch(), this.properties.getId());
		this.properties.setMyVote(myVote123);
		
		Notification myNotification = new Notification(this.properties.getMyVote(), this.properties.getElectionRound(), this.properties.getId(), this.properties.getNodestate());
		LOG.info("My Notification is:"+myNotification.toString());
		
		sendNotification(memberList, myNotification, currentElectionQueue); 
		Notification currentN = null;
		while( properties.getNodestate() == NodeServerProperties.State.ELECTION && timeout<limit_timeout ){
			LOG.info("ElectionQueue:\n"+ this.getProperties().getElectionQueue());
			currentN = currentElectionQueue.poll();
			
			if(currentN==null){
				LOG.info("Queue is empty!!");
				try {
					synchronized (currentElectionQueue) {
						currentElectionQueue.wait(timeout);
	                }
					currentN = currentElectionQueue.poll();
					
					if(currentN==null){
						LOG.info("Queue is empty again!!");
						timeout = 2*timeout;
						LOG.info("increasing timeout");
						sendNotification(memberList, myNotification, currentElectionQueue);
						continue;
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			//CurrentN is not null
			
			else if ( currentN.getSenderState() == NodeServerProperties.State.ELECTION ){
				LOG.info("Received notification is in Election");
				if(currentN.getSenderRound() < this.properties.getElectionRound()){
					LOG.info("Disregard vote as round number is smaller than mine");
					continue;
				}else{
					if(currentN.getSenderRound() > this.properties.getElectionRound()){
						LOG.info("The round number is larger than mine");
						this.properties.setElectionRound(currentN.getSenderRound());
						
						receivedVote = new HashMap<Long, Vote>();
						receivedVotesRound = new HashMap<Long, Long>();
					}
					LOG.info("-------------------------");
					LOG.info("myvote:"+this.properties.getMyVote());
					LOG.info("othervote:"+currentN.getVote());
					LOG.info("vote compare:"+ currentN.getVote().compareTo(this.properties.getMyVote()));
					LOG.info("-------------------------");
					if(currentN.getVote().compareTo(this.properties.getMyVote()) > 0 ){ // if the currentN is bigger thn myvote
						LOG.info("His vote bigger than mine");
						this.properties.setMyVote(currentN.getVote()); // update myvote
						myNotification.setVote(this.properties.getMyVote()); // update notification
						
					}
					sendNotification(memberList, myNotification, currentElectionQueue);
					// update the receivedVote datastructure
					receivedVote.put(currentN.getSenderId(), currentN.getVote());
					receivedVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
					//TODO shoul i put my vote in the receivedVote
					receivedVote.put(this.properties.getId(), this.properties.getMyVote());
					receivedVotesRound.put(this.properties.getId(), this.properties.getElectionRound());
					
					if(receivedVote.size() == (memberList.size()+1)){
						//TODO check for quorum in the receivedvotes and then declare leader
						LOG.info("received votes from all the members");
						break;
					}
					else {
						LOG.info("checking for quorum in received votes");
						int myVoteCounter = 0;
						for( Entry<Long, Vote> v:receivedVote.entrySet()){
							Vote currVote = v.getValue();
							if(currVote.equals(this.properties.getMyVote())){
								myVoteCounter++;
							}
						}
						if(myVoteCounter> (memberList.size()+1)/2 ){
							LOG.info("Found  quorum in received votes");
							try {
								synchronized (currentElectionQueue) {
									currentElectionQueue.wait(2*timeout);
				                }
								
//								Thread.sleep(timeout);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if(properties.getElectionQueue().size() > 0) {
								LOG.info("Still have notifications in ElectionQueue");
								continue; }
							else {
								LOG.info("No notifications in ElectionQueue");
								break;
							}
						}
						else{
							continue;
						}
					}
					
				}
				
				
			
			
			} //end of if election
			
			// the received vote is either leading or following
			else{
				if(currentN.getSenderRound() == this.properties.getElectionRound()){
					LOG.info("notification is not in election, round number are same");
					receivedVote.put(currentN.getSenderId(), currentN.getVote());
					receivedVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
					//TODO shoul i put my vote in the receivedVote
//					receivedVote.put(this.properties.getId(), myVote);
//					receivedVotesRound.put(this.properties.getId(), this.electionRound);
					
					if(currentN.getSenderState() == NodeServerProperties.State.LEADING){
						LOG.info("notification is not in Leading state");
						this.properties.setMyVote( currentN.getVote());
						break;
					}
					else{
						LOG.info("notification is not in Followinf state");
						int myVoteCounter = 0;
						for( Entry<Long, Vote> v:receivedVote.entrySet()){
							Vote currVote = v.getValue();
							if(currVote.equals(this.properties.getMyVote())){
								myVoteCounter++;
							}
						}
						// if the currentN's vote is to me and i achieve quorum in receivedVote then i be the leader
						
						if(currentN.getVote().getId()==this.properties.getMyVote().getId() && myVoteCounter> (memberList.size()+1)/2 ){
							
							this.properties.setMyVote(currentN.getVote());
								break;
							
						}
						else if(myVoteCounter> (memberList.size()+1)/2 ){  //our improvement
							
							this.properties.setMyVote(currentN.getVote());
							break;
						
						}
						//wrong condition
//						else if(myVoteCounter> (memberList.size()+1)/2 
//								&& OutOfElectionVotes.containsKey(currentN.getVote().getId())){
//							//TODO this is not 100% sure
//							myVote = currentN.getVote();
//							break;
//						}
						
					}
					
				}
				
				OutOfElectionVotes.put(currentN.getSenderId(), currentN.getVote());
				OutOfElectionVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
				
				int myVoteCounter = 0;
				for( Entry<Long, Vote> v:OutOfElectionVotes.entrySet()){
					Vote currVote = v.getValue();
					if(currVote.equals(this.properties.getMyVote())){
						myVoteCounter++;
					}
				}
				
				if(currentN.getVote().getId()==this.properties.getMyVote().getId() && myVoteCounter> (memberList.size()+1)/2 ){
					
					this.properties.setElectionRound(currentN.getSenderRound());
					this.properties.setMyVote(currentN.getVote());
					break;
				}
				else if(myVoteCounter> (memberList.size()+1)/2 ){  //our improvement just chekc for the quorum
					
					this.properties.setMyVote(currentN.getVote());
					break;
				
				}
				//wrong condition
//				else if(myVoteCounter> (memberList.size()+1)/2 && OutOfElectionVotes.containsKey(currentN.getVote().getId())){
//					this.electionRound = currentN.getSenderRound();
//					
//				}
				
				
				
			} //end of else
			
			
			
			
			
		}// end of while
		// Here the leader is the one pointed by my vote
		
		return this.properties.getMyVote();
		
	}

	private void sendNotification(List<InetSocketAddress> memberList2, Notification myNotification, ConcurrentLinkedQueue<Notification> currentElectionQueue) {
		if(memberList2.isEmpty()) return;
		
		
		for(InetSocketAddress member: memberList2){
			SendNotificationThread nt0 = new SendNotificationThread(member, myNotification);
			nt0.setElectionQueue1(currentElectionQueue);
			Thread t = new Thread(nt0);
			t.start();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
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
