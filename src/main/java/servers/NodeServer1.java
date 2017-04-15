package servers;

import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import netty.NettyClient1;
import netty.NettyServer1;
import util.UdpClient1;
import util.UdpServer1;

public class NodeServer1 {
	private static final Logger LOG = LogManager.getLogger(NodeServer1.class);
	private NodeServerProperties1 properties;
	private Thread udpServerThread;
	private Thread nettyServerThread;
	private Thread udpClientThread;
	private NettyClient1 nettyClient;
	

	public NodeServer1(NodeServerProperties1 properties) {
		super();
		this.properties = properties;
	}

	public NodeServer1(String bhost, int bport, int nport) {
		super();
		this.properties = new NodeServerProperties1();
		this.properties.setBootstrapHost(bhost);
		this.properties.setBootstrapPort(bport);
		this.properties.setNodePort(nport);
	}

	private Vote startLeaderElection() {
		
		List<InetSocketAddress> memberList = this.properties.getMemberList();
		HashMap<Long, Vote> receivedVote = new HashMap<Long, Vote>();
		HashMap<Long, Long> receivedVotesRound = new HashMap<Long, Long>();
		HashMap<Long, Vote> OutOfElectionVotes = new HashMap<Long, Vote>();
		HashMap<Long, Long> OutOfElectionVotesRound = new HashMap<Long, Long>();
		MpscArrayQueue<Notification> currentElectionQueue = this.properties.getElectionQueue();
		Vote myVote = new Vote(this.properties.getLastZxId(),this.properties.getNodeId());
		
		long limit_timeout = 10000;
		long timeout = 1000;
		
		this.properties.setElectionRound(this.properties.getElectionRound()+1);
		this.properties.setMyVote(myVote);
		
		Notification myNotification = new Notification(this.properties.getMyVote(), this.properties.getNodeId(), this.properties.getNodestate(), this.properties.getElectionRound());
		LOG.info("My Notification is:"+myNotification.toString());
		
		//TODO: Verify
		sendNotificationToAll(myNotification.toString()); 
		
		while(this.properties.getNodestate() == NodeServerProperties1.State.ELECTION && timeout<limit_timeout ){
			LOG.info("Fetching from CurrentElectionQueue:\n");
			Notification currentN = currentElectionQueue.poll();
			
			if(currentN==null){
				LOG.info("Notification Queue is empty!!");
				try {
					synchronized (currentElectionQueue) {
						currentElectionQueue.wait(timeout);
	                }
					currentN = currentElectionQueue.poll();
					
					if(currentN==null){
						LOG.info("Notification Queue is empty again!!");
						timeout = 2*timeout;
						LOG.info("increasing timeout");
						//TODO: verify
						sendNotificationToAll(myNotification.toString()); 
						
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			//CurrentN is not null
			
			else if ( currentN.getSenderState() == NodeServerProperties1.State.ELECTION ){
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
					//TODO: verify
					sendNotificationToAll(myNotification.toString());
					
					// update the receivedVote datastructure
					receivedVote.put(currentN.getSenderId(), currentN.getVote());
					receivedVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
					//TODO should I put my vote in the receivedVote
					receivedVote.put(this.properties.getNodeId(), this.properties.getMyVote());
					receivedVotesRound.put(this.properties.getNodeId(), this.properties.getElectionRound());
					
					if(receivedVote.size() == (memberList.size()+1)){
						//TODO check for Quorum in the receivedvotes and then declare leader
						LOG.info("***Received Votes from all the members");
						break;
					}
					else {
						LOG.info("*Checking for Quorum in received votes");
						int myVoteCounter = 0;
						for( Entry<Long, Vote> v:receivedVote.entrySet()){
							Vote currVote = v.getValue();
							if(currVote.equals(this.properties.getMyVote())){
								myVoteCounter++;
							}
						}
						if(myVoteCounter> (memberList.size()+1)/2 ){
							LOG.info("**Found Quorum in received votes");
							try {
								synchronized (currentElectionQueue) {
									currentElectionQueue.wait(timeout);
									Thread.sleep(timeout);
				                }
								
//								Thread.sleep(timeout);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if(currentElectionQueue.size() > 0) {
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
			
			else {	// the received vote is either leading or following
				if(currentN.getSenderRound() == this.properties.getElectionRound()){
					LOG.info("Notification is not in election, round numbers of current node and current notification are same");
					receivedVote.put(currentN.getSenderId(), currentN.getVote());
					receivedVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
					//TODO should i put my vote in the receivedVote
//					receivedVote.put(this.properties.getId(), myVote);
//					receivedVotesRound.put(this.properties.getId(), this.electionRound);
					
					if(currentN.getSenderState() == NodeServerProperties1.State.LEADING){ //This is notification from leader
						LOG.info("This notification is from the Leader");
						this.properties.setMyVote( currentN.getVote());
						break;
					}
					else{
						LOG.info("This notification is from a Follower");
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
				else if(myVoteCounter> (memberList.size()+1)/2 ){  //our improvement just check for the quorum
					
					this.properties.setMyVote(currentN.getVote());
					break;
				
				}
//				//wrong condition
//				else if(myVoteCounter> (memberList.size()+1)/2 && OutOfElectionVotes.containsKey(currentN.getVote().getId())){
//					this.electionRound = currentN.getSenderRound();
//					
//				}				

				
			} //end of else
						

			
			
		}// end of while
//		// Here the leader is the one pointed by my vote
//
		if(this.properties.getMyVote().getId() == this.properties.getNodeId()){
			this.properties.setLeader(true);
			this.properties.setLeaderId(this.properties.getNodeId());
			this.properties.setNodestate(NodeServerProperties1.State.LEADING);
		} else {
			this.properties.setLeader(false);
			this.properties.setLeaderId(this.properties.getMyVote().getId());
			this.properties.setNodestate(NodeServerProperties1.State.FOLLOWING);
		}

		this.properties.setAcceptedEpoch(this.properties.getLastZxId().getEpoch());
		return this.properties.getMyVote();
//		
	}

	private void Recovery() {

		if (this.properties.isLeader() == true){
			// Leader
			ConcurrentHashMap<Long, Long> acceptedEpochMap =  this.properties.getSynData().getAcceptedEpochMap();
			ConcurrentHashMap<Long, ZxId> currentEpochMap =  this.properties.getSynData().getCurrentEpochMap();

			acceptedEpochMap.clear();
			currentEpochMap.clear();

			this.properties.getSynData().setAcceptedEpochMap(acceptedEpochMap);
			this.properties.getSynData().setCurrentEpochMap(currentEpochMap);

			acceptedEpochMap =  this.properties.getSynData().getAcceptedEpochMap();

			while(acceptedEpochMap.size() < this.properties.getMemberList().size()/2 ){
				//TODO: Figure out how to update memberlist size
				try {
					Thread.sleep(10);
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}

			long max = this.properties.getAcceptedEpoch();
			for(long nodeId : acceptedEpochMap.keySet()){
				long accEpoch =  acceptedEpochMap.get(nodeId);
				if (accEpoch > max){
					max = accEpoch;
				}
			}

			this.properties.setNewEpoch(max + 1);

			String newEpochmsg = "NEWEPOCH:" + this.properties.getNewEpoch();
			this.broadcast(newEpochmsg);

			ZxId leaderLastCommittedZxid = readHistory();

			currentEpochMap =  this.properties.getSynData().getCurrentEpochMap();

			while(currentEpochMap.size() < this.properties.getMemberList().size()/2 ){
				//TODO: Figure out how to update memberlist size
				try {
					Thread.sleep(10);
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}

			for (long nodeId : currentEpochMap.keySet()){
				ZxId followerLastCommittedZxid = currentEpochMap.get(nodeId);

				if (leaderLastCommittedZxid.getEpoch() == followerLastCommittedZxid.getEpoch()){

					if (followerLastCommittedZxid.getCounter() < leaderLastCommittedZxid.getCounter()){

						// TODO: Send DIFF message
						// Iterate through CommitHistory (refer readHistory()), stringify and send

					} else if (followerLastCommittedZxid.getCounter() == leaderLastCommittedZxid.getCounter()){
						continue;
					} else if (followerLastCommittedZxid.getCounter() > leaderLastCommittedZxid.getCounter()){
						// Go to Leader Election. Ideally, shouldn't happen
						this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
						changePhase();
					}

				} else if (followerLastCommittedZxid.getEpoch() < leaderLastCommittedZxid.getEpoch()){

					// TODO: Send SNAP message
					// Iterate through the Map, stringify each entry and then send

				} else if (followerLastCommittedZxid.getEpoch() > leaderLastCommittedZxid.getEpoch()){
					// Go to Leader Election. Ideally, shouldn't happen
					this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
					changePhase();
				}
			}

		} else {
			// Follower
			ZxId followerLastCommittedZxid = readHistory();
			long currentEpoch = followerLastCommittedZxid.getEpoch();
			this.properties.setCurrentEpoch(currentEpoch);

			String followerinfomsg = "FOLLOWERINFO:" + this.properties.getNodeId() + ":"
					+ this.properties.getAcceptedEpoch() + ":"
					+ readHistory().getEpoch() + ":" + readHistory().getCounter();
			this.nettyClient.sendMessage(leaderip, leaderport, followerinfomsg);
			//TODO: Figure out how follower received newEpoch
			long newEpoch = this.properties.getSynData().getNewEpoch();
			long acceptedEpoch = this.properties.getAcceptedEpoch();


			if (newEpoch > acceptedEpoch){
				this.properties.setAcceptedEpoch(newEpoch);
				this.properties.setCounter(0);
				String ackepochmsg = "ACKEPOCH:" + this.properties.getCurrentEpoch(); // TODO: currentepoch, history, lastZxid
				this.nettyClient.sendMessage(leaderip, leaderport, ackepochmsg);
			} else {
				this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
				changePhase();
			}

			// Receive SNAP, DIFF and TRUNC messages



		}

	}
	
	
	public void init() {
		LOG.info("Starting the Node server");

		msgBootstrap();

		LOG.info("ID for this node is :" + properties.getNodeId());

		this.udpServerThread = new Thread(new UdpServer1(properties));
		this.udpServerThread.setPriority(Thread.MIN_PRIORITY);
		this.udpServerThread.start();

		this.nettyServerThread = new Thread(new NettyServer1(properties.getNodePort(), properties));
		this.nettyServerThread.setPriority(Thread.MIN_PRIORITY);
		this.nettyServerThread.start();

		this.udpClientThread = new Thread(new UdpClient1(properties));
		this.udpClientThread.setPriority(Thread.MIN_PRIORITY);
		this.udpClientThread.start();

		this.nettyClient = new NettyClient1(properties);
		joinGroup();

		readHistory();
		startLeaderElection();
		//changePhase();

	}


	private void changePhase() {

			while(true){
			/*Thread leaderElectionThread = new Thread(target);
			leaderElectionThread.start();
			leaderElectionThread.join();
			
			Thread recoveryThread = new Thread(target);
			recoveryThread.start();
			recoveryThread.join();
			
			Thread broadcastThread = new Thread(target);
			broadcastThread.start();
			broadcastThread.join();*/
				
				Vote leaderVote = startLeaderElection();
				Recovery();
				//Broadcast();
			
			}
	}

	public long msgBootstrap() {
		Socket socket;
		long id = 0;
		try {
			socket = new Socket(properties.getBootstrapHost(), properties.getBootstrapPort());

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// set self_ip:port to bootstrap

			out.println("set " + properties.getNodeHost() + ":" + properties.getNodePort());

			String memberList = in.readLine();
			String memberId = in.readLine();
			id = Long.parseLong(memberId);
			LOG.info("MemberID received:" + id);
			// process memberlist

			this.properties.setNodeId(id);

			parseMemberList(memberList);

			out.close();
			in.close();
			socket.close();

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
		for (String s : list) {
			String[] address = s.split(":");
			String ip = address[0];
			int port = Integer.parseInt(address[1]);
			if (properties.getNodeHost().equals(ip) && properties.getNodePort() == port) {
			} else {
				InetSocketAddress addr = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
				properties.addMemberToList(addr);

			}

		}

	}

	private void sendNotificationToAll(String message){
		LOG.info("SendNotificationToAll()"+ message);
		broadcast("CNOTIFICATION:" + message);
	}
	
	private void joinGroup() {
		
		broadcast("JOIN_GROUP:"+ properties.getNodeHost() + ":" + properties.getNodePort());
//		List<InetSocketAddress> unreachablelist = new ArrayList<InetSocketAddress>();
//		
//		for (InetSocketAddress member : properties.getMemberList()) {
//
//			try {
//				this.nettyClient.sendMessage(member.getHostName(), member.getPort(),
//						"JOIN_GROUP:" + properties.getNodeHost() + ":" + properties.getNodePort());
//			} catch (Exception e) {
//				unreachablelist.add(member);
//				e.printStackTrace();
//			}
//
//		}
//
//		for (InetSocketAddress member : unreachablelist) {
//			properties.removeMemberFromList(member);
//		}

	}

	private void broadcast(String message) {

		LOG.info("Broadcast"+ message);
		List<InetSocketAddress> unreachablelist = new ArrayList<InetSocketAddress>();
		for (InetSocketAddress member : properties.getMemberList()) {
			try {
				
				this.nettyClient.sendMessage(member.getHostName(), member.getPort(), message);
			}
			catch (Exception e) {
				unreachablelist.add(member);
				e.printStackTrace();
			}
		}

		for (InetSocketAddress member : unreachablelist) {
			properties.removeMemberFromList(member);
		}
	}
	
	private ZxId readHistory() {

		String fileName = "CommitedHistory_" + properties.getNodePort() + ".txt";
		String line = null;
		Queue<Message> msgList = properties.getCommitQueue();
		try {

			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
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

		Message[] msgArr = new Message[msgList.size()];
		msgList.toArray(msgArr);

		Message lastMsg = msgArr[msgArr.length - 1];

		this.properties.setCurrentEpoch(lastMsg.getZxid().getEpoch());
		this.properties.setLastZxId(lastMsg.getZxid());

		return this.properties.getLastZxId();

	}

}
