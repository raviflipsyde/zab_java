package netty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import servers.*;

import util.FileOps;

/**
 * Handles a server-side channel.
 */
public class InHandler2 extends ChannelInboundHandlerAdapter { // (1)
	private static final Logger LOG = LogManager.getLogger(InHandler2.class);
	
	private NodeServerProperties1 properties;

	private NettyClient1 nettyClientInhandler;


	public InHandler2(NodeServerProperties1 nsProperties) {
		this.properties = nsProperties;
		this.nettyClientInhandler = new NettyClient1(this.properties);
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		ByteBuf in = (ByteBuf) msg;
		String requestMsg  = in.toString(StandardCharsets.UTF_8 );
		LOG.debug(">>>Channel Read:" + requestMsg);
		String response = handleClientRequest(requestMsg);
		LOG.debug("<<<Response:" + response);
		
		if(response.length()>0){
			ctx.write(Unpooled.copiedBuffer(response+"\r\n", StandardCharsets.UTF_8));
			ctx.flush(); // (2)
			
		}
		ctx.close();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

		super.channelRegistered(ctx);
		LOG.debug("Channel Registered: "+ ctx.channel().localAddress() + ":" + ctx.channel().remoteAddress());
	}


	private String handleClientRequest(String requestMsg) {
		//		LOG.debug("handleClientRequest:"+requestMsg);
		
		if(requestMsg.contains("READ:")){
			LOG.info("Received: " + requestMsg);
//			LOG.info("Node ID:" + this.properties.getNodeId() + "received Read() request from client");
			String[] arr = requestMsg.split(":");
			String key = arr[1].trim();
			Properties properties = this.properties.getDataMap();
			if(properties.containsKey(key)){
				String value = properties.getProperty(key);
				LOG.info("Returning from local replica: Reply:" + key + value);
				return value;
			}
			else{
				return "READ ERROR: No result for key:" + key;
			}
				
		}
		
		if(requestMsg.contains("WRITE:")){
			
			LOG.info("Received: " + requestMsg);
			
			if(!properties.isLeader()){ //follower
				//Forward write request to the leader
				LOG.debug("Follower received WRITE request from client, forwarding to the leader..!!");
				this.nettyClientInhandler.sendMessage(properties.getLeaderAddress().getHostName(), properties.getLeaderAddress().getPort(), requestMsg);
				return "OK";
				
			}
			else{ //leader

				//"WRITE:KEY:VALUE"
				String[] arr = requestMsg.split(":");
				
				//Key-value pair to be proposed
				String key = arr[1].trim();
				String value = arr[2].trim();
				long epoch = this.properties.getAcceptedEpoch();
				synchronized(new Long(properties.getCounter())){
					long counter = this.properties.getCounter();
					counter++;
					this.properties.setCounter(counter);
				}
			
				
				//Form a proposal
				ZxId z = new ZxId(epoch, this.properties.getCounter());
				Proposal p = new Proposal(z, key, value);
				String proposal = "PROPOSE:" + p.toString();
				
				//enqueue this proposal to proposed transactions to keep the count of Acknowledgements
				
				SortedMap<Proposal, AtomicInteger> proposedtransactions = properties.getSynData().getProposedTransactions();
				proposedtransactions.put(p, new AtomicInteger(1));
				
				//checking if the entry is enqueued in the proposed transaction map
				LOG.debug("Checking the counter right after enqueueing the entry: " + proposedtransactions.get(p));
				
				//send proposal to quorum
				LOG.info("Leader sending: " + proposal);
				
				LOG.debug("Number of members:" + properties.getMemberList().size());
				
				for (Entry<Long, InetSocketAddress> member : properties.getMemberList().entrySet()) {
						LOG.debug("Sending "+proposal+" to: "+ member.getValue().getHostName() + ":"+ member.getValue().getPort());
						this.nettyClientInhandler.sendMessage(member.getValue().getHostName(), member.getValue().getPort(), proposal);
				}

			}
		
			return "OK";
		}
		
		if(requestMsg.contains("PROPOSE")){
			if(properties.isLeader()){ // Leader will not accept this message
				LOG.debug("I am the Leader, I do not accept proposals");
				return "ERROR: I am the eader, I send proposals, not accept!";
				
			}
			else{ ///Follower
				//enqueue this message to proposal queue 
				LOG.info("Received: " + requestMsg);
				
				String[] arr = requestMsg.split(":");
				Long epoch = Long.parseLong(arr[1].trim());
				Long counter = Long.parseLong(arr[2].trim());
				ZxId z = new ZxId(epoch, counter);
				String key = arr[3].trim();
				String value = arr[4].trim();
				Proposal proposal = new Proposal(z,key,value);
				
				properties.getSynData().getProposedTransactions().put(proposal, new AtomicInteger(0));
				LOG.debug("Enqueing proposal in Proposal Queue:" + proposal);
				
				LOG.debug("Sending Acknowledgement to the leader");
				return "ACK_PROPOSAL:" + proposal.toString();
		
			}
		}
		
		if(requestMsg.contains("ACK_PROPOSAL")){
			if(!properties.isLeader()){//follower
				//follower should disregard this message
				LOG.debug("Follower got ACK_PROPOSAL, shouldn't happen!");
				return "ERROR:Follower got ACK_PROPOSAL";
			}
			else{//Leader
				String[] arr = requestMsg.split(":");
				
				//Parsing proposal for which acknowledgement was received				
				Long epoch = Long.parseLong(arr[1].trim());
				Long counter = Long.parseLong(arr[2].trim());
				ZxId z = new ZxId(epoch, counter);
				String key = arr[3].trim();
				String value = arr[4].trim();
				Proposal p = new Proposal(z,key,value);
				
				//we have to increment the ack count for this zxid
				LOG.debug("Leader: Got ACK_PROPOSAL, incrementing count for zxid" + z);
				
				//checking the ack count for the proposal (counter value)		
				SortedMap<Proposal, AtomicInteger> proposedtransactions = properties.getSynData().getProposedTransactions();
				
				synchronized (proposedtransactions) {
					int count = proposedtransactions.get(p).incrementAndGet();
					proposedtransactions.put(p, new AtomicInteger(count));
				}
				//LOG.debug("##################ACK count for proposal before incrementing####################" + proposedtransactions.get(p));
//				int count = proposedtransactions.get(p).incrementAndGet();
//				proposedtransactions.put(p, new AtomicInteger(count));
				//LOG.debug("###################ACK count for proposal after incrementing####################" + proposedtransactions.get(p));
				
				return "OK";
			}
			
		}
		
		if(requestMsg.contains("COMMIT:")){
			if(properties.isLeader()){ // leader will not accept this message
				LOG.debug("I am the Leader, I do not accept commit messages");
				return "ERROR: I am the eader, I send proposals, not accept!";
			}
			else{//follower
				LOG.debug ("Follower received COMMIT message");
				LOG.info ("-----Received: " + requestMsg);
				String[] arr = requestMsg.split(":");
				
				//Parsing proposal for which acknowledgement was received				
				Long epoch = Long.parseLong(arr[1].trim());
				Long counter = Long.parseLong(arr[2].trim());
				ZxId z = new ZxId(epoch, counter);
				String key = arr[3].trim();
				String value = arr[4].trim();
				Proposal p = new Proposal(z,key,value);
				SortedMap<Proposal, AtomicInteger> proposalMap = properties.getSynData().getProposedTransactions();
				LOG.debug("Map Size when Commit received: "+proposalMap.size());
				LOG.debug("Map when Commit received: "+ proposalMap);
				
				if(proposalMap.containsKey(p)){
					LOG.debug("Commit Queue contains the transaction to be removed:" + p);
					//String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
					//FileOps.appendTransaction(fileName, p.toString());
					synchronized (properties.getSynData().getProposedTransactions()) {
						//remove from proposedtransactions map
						LOG.debug("Inside synchronized block....!!!");
						properties.getSynData().getProposedTransactions().remove(p);
						
						//enqueue in commitQueue
						properties.getSynData().getCommittedTransactions().add(p);
					}				
				}	
				
				return "OK";
			}
		}
		
		
		if(requestMsg.contains("JOIN_GROUP:")){
			//add the ip:port to the group member list;


			String[] arr = requestMsg.split(":");
			long nodeId = Integer.parseInt(arr[1].trim());
			InetSocketAddress addr = new InetSocketAddress(arr[2].trim(), Integer.parseInt(arr[3].trim()));
			properties.addMemberToList(nodeId, addr);

			LOG.debug(properties.getMemberList());

			return "OK";
		}

		if(requestMsg.contains("CNOTIFICATION:")){
			//add the ip:port to thefore group member list;
			

			String[] arr = requestMsg.split(":");

			Notification responseNotification = new Notification(arr[1].trim());
			
			if(properties.getNodestate() == NodeServerProperties1.State.ELECTION){
				
				MpscArrayQueue<Notification> currentElectionQueue = properties.getSynData().getElectionQueue();
				LOG.debug("Before:"+currentElectionQueue.size());
				LOG.debug("adding notification to the queue"+ responseNotification.toString());
				currentElectionQueue.offer(responseNotification);
				
					try {
						
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				LOG.debug("After:"+currentElectionQueue.size());
				LOG.debug("NODE is in STATE: "+ properties.getNodestate());
				LOG.debug("My Election ROUND: "+ properties.getElectionRound());
				LOG.debug("his Election ROUND: "+ responseNotification.getSenderRound());
				
			if(responseNotification.getSenderState() == NodeServerProperties1.State.ELECTION
						&& responseNotification.getSenderRound() <= properties.getElectionRound()){

					// get my current vote from FLE or when FLE is underway
					Vote myVote = properties.getMyVote();
					//public Notification(Vote vote, long id, servers.NodeServerProperties1.State state, long round)
					Notification myNotification = new Notification(myVote, properties.getNodeId(), properties.getNodestate(), properties.getElectionRound());
					return("SNOTIFICATION:"+myNotification.toString());

				}
			}
			else if(responseNotification.getSenderState() == NodeServerProperties1.State.ELECTION){
					// get my current vote from FLE or when FLE is underway
					Vote myVote = properties.getMyVote();

					Notification myNotification = new Notification(myVote, properties.getNodeId(), properties.getNodestate(), properties.getElectionRound());
					LOG.debug("myNotification:"+myNotification);
					return("SNOTIFICATION:"+myNotification.toString());

				}

			
			
			return("");

		}
		
		
		if(requestMsg.contains("SNOTIFICATION:")){
			//add the ip:port to the group member list;


			String[] arr = requestMsg.split(":");

			Notification responseNotification = new Notification(arr[1].trim());
			
			if(properties.getNodestate() == NodeServerProperties1.State.ELECTION){
				MpscArrayQueue<Notification> currentElectionQueue = properties.getSynData().getElectionQueue();
				
				LOG.debug("Before:"+currentElectionQueue.size());
				currentElectionQueue.offer(responseNotification);
				
					try {
						
						Thread.sleep(1000);
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			
				LOG.debug("After:"+currentElectionQueue.size());


			}
			


			LOG.debug(properties.getMemberList());
			return("ERROR");

		}
		
		if(requestMsg.contains("OK")){
			//add the ip:port to the group member list;


			//			String[] arr = requestMsg.split(":");
			//			
			//			InetSocketAddress addr = new InetSocketAddress(arr[1].trim(), Integer.parseInt(arr[2].trim()));
			//			server.addMemberToList(addr);
			LOG.debug("Client received OK!!");
			LOG.debug(properties.getMemberList());

			return "";
		}

		if (requestMsg.contains("FOLLOWERINFO")){
			LOG.info("Leader Received: " + requestMsg);
			
			while(properties.getNodestate() == NodeServerProperties1.State.ELECTION){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			String[] accEpoch = requestMsg.split(":");
			long nodeId = Long.parseLong(accEpoch[1].trim());
			long acceptedEpoch = Long.parseLong(accEpoch[2].trim());
//			long currentEpoch = Long.parseLong(accEpoch[3].trim());
//			long currentCounter = Long.parseLong(accEpoch[4].trim());

			//ZxId followerLastCommittedZxid = new ZxId(currentEpoch, currentCounter);

			ConcurrentHashMap<Long, Long> acceptedEpochMap = this.properties.getSynData().getAcceptedEpochMap();
			//ConcurrentHashMap<Long, ZxId> currentEpochMap = properties.getSynData().getCurrentEpochMap();
			
			synchronized (acceptedEpochMap){
				acceptedEpochMap.put(nodeId, acceptedEpoch);
			}
			//currentEpochMap.put(nodeId, followerLastCommittedZxid);

			if (acceptedEpochMap.size() < (this.properties.getMemberList().size() / 2) ){
				synchronized (acceptedEpochMap){
					try {
						acceptedEpochMap.wait(100);
						
					} catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}

			while (!this.properties.getSynData().isNewEpochFlag()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}
			
			String newEpoch = "NEWEPOCH:" + properties.getNewEpoch() + ":" + this.properties.getNodeId(); 
			LOG.info("Send: " + newEpoch);
			return newEpoch;
		}

		if (requestMsg.contains("NEWEPOCH")){

			Map<Long, InetSocketAddress> memberList = this.properties.getMemberList();
			LOG.debug("Member List is = " + memberList);

			String ackepochmsg = "";
			String[] newEpocharr = requestMsg.split(":");
			long newEpoch = Long.parseLong(newEpocharr[1].trim());
			long nodeId = Long.parseLong(newEpocharr[2].trim());
			properties.getSynData().setNewEpoch(newEpoch);
			LOG.info("New Epoch Received : " + newEpoch);
			this.properties.setNewEpoch(newEpoch);
			long acceptedEpoch = this.properties.getAcceptedEpoch();

			if (newEpoch > acceptedEpoch){

				this.properties.setAcceptedEpoch(newEpoch);
				this.properties.setCounter(0);

				String myLastLog = FileOps.readLastLog(properties);
				String[] arr = myLastLog.split(",");
				long currentEpoch = Long.parseLong(arr[0].trim());
				long currentCounter = Long.parseLong(arr[1].trim());

				ackepochmsg = "ACKEPOCH:" + this.properties.getNodeId()
						+ ":" + currentEpoch + ":" + currentCounter;
				
				LOG.info("Send: " + ackepochmsg);
				this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(), memberList.get(nodeId).getPort(), ackepochmsg);

				return "";

			} else {

				this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
				LOG.debug("Going to Leader Election");

			}

		}

		if (requestMsg.contains("ACKEPOCH")){
			LOG.info("Leader Received: " + requestMsg);
			Map<Long, InetSocketAddress> memberList = this.properties.getMemberList();
			String[] currEpochArr = requestMsg.split(":");
			long nodeId = Long.parseLong(currEpochArr[1].trim());
			long currentEpoch = Long.parseLong(currEpochArr[2].trim());
			long currentCounter = Long.parseLong(currEpochArr[3].trim());

			ZxId followerLastCommittedZxid = new ZxId(currentEpoch, currentCounter);
			LOG.debug("follower last committed zxid = " + followerLastCommittedZxid.getEpoch()
					+ "	" + followerLastCommittedZxid.getCounter());

//			ConcurrentHashMap<Long, ZxId> currentEpochMap = properties.getSynData().getCurrentEpochMap();
//			currentEpochMap.put(nodeId, followerLastCommittedZxid);

			String leaderLastLog = FileOps.readLastLog(properties);
			LOG.debug("Leader last log = " + leaderLastLog);
			String[] arr1 = leaderLastLog.split(",");
			long epoch = Long.parseLong(arr1[0].trim());
			long counter = Long.parseLong(arr1[1].trim());

			ZxId leaderLastCommittedZxid = new ZxId(epoch, counter);
			LOG.debug("leader last committed zxid = " + leaderLastCommittedZxid.getEpoch()
					+ "	" + leaderLastCommittedZxid.getCounter());

			if (leaderLastCommittedZxid.getEpoch()==followerLastCommittedZxid.getEpoch() ){

				if (followerLastCommittedZxid.getCounter() < leaderLastCommittedZxid.getCounter()){

					String diffMsg = "";
					List<String> logList= FileOps.getDiffResponse(properties, followerLastCommittedZxid);

					diffMsg = "DIFF:" + logList;
					LOG.debug("Send: DIFF"+diffMsg);
					LOG.info("Send: DIFF");
					this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(),
							memberList.get(nodeId).getPort(), diffMsg);


				} else if (followerLastCommittedZxid.getCounter() == leaderLastCommittedZxid.getCounter()){

					this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(),
							memberList.get(nodeId).getPort(), "NEXT");


				} else if (followerLastCommittedZxid.getCounter() > leaderLastCommittedZxid.getCounter()){
					// Go to Leader Election. Ideally, shouldn't happen
					this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
					LOG.debug("Going to Leader Election");
					// change phase
				}

			} 
			else if (leaderLastCommittedZxid.getEpoch()-followerLastCommittedZxid.getEpoch() < 4 ){

					String diffMsg = "";
					List<String> logList= FileOps.getDiffResponse(properties, followerLastCommittedZxid);

					diffMsg = "DIFF:" + logList;
					LOG.debug("Send: "+diffMsg);
					LOG.info("Send: DIFF");
					this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(),
							memberList.get(nodeId).getPort(), diffMsg);

				

			} 
			else if (followerLastCommittedZxid.getEpoch() > leaderLastCommittedZxid.getEpoch()){
				// Go to Leader Election. Ideally, shouldn't happen
				this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
				LOG.debug("Going to Leader Election");
				//changePhase();
			}
			else {


				String snapmsg = "SNAP:" + properties.getDataMap();
				LOG.info("Send: "+snapmsg);
				this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(), memberList.get(nodeId).getPort(), snapmsg);
				

			} 

			return "";

		}

		if (requestMsg.contains("DIFF")){
			LOG.info("DIFF message received");

			String[] decodedDiff = requestMsg.split(":");
			LOG.debug("Diff decoded is = " + decodedDiff[0] + "	" + decodedDiff[1]);
			//REmove all brackets and split on multiple spaces
			String[] diff = decodedDiff[1].replaceAll("\\[", "").replaceAll("\\]", "").split(", ");

			for (int i = 0; i < diff.length; i++){
				//Remove last comma
				diff[i] = diff[i].trim();
				if (diff[i].length() == 0) break;
				LOG.debug("Diff i = " + i + " Diff[i] = " + diff[i]);
				String[] proposalArr = diff[i].split(",");
				long epoch = Long.parseLong(proposalArr[0].trim());
				long counter = Long.parseLong(proposalArr[1].trim());
				String key = proposalArr[2].trim();
				String value = proposalArr[3].trim();
				ZxId zxid = new ZxId(epoch, counter);
				Proposal pro = new Proposal(zxid, key, value);
				//If it doesn't exist, create one.
				String resp = FileOps.appendTransaction(properties, pro.toString());
				
				//this.properties.getSynData().getCommitQueue().offer(pro);
				
			}
			
			
			FileOps.writeDataMap(properties);
			
			String readyMsg = "READY:" + this.properties.getNodeId(); 
			LOG.info("Send: "+readyMsg );
			long reEndTime = System.nanoTime();
			LOG.info("Time required for Recovery Phase:" + (reEndTime-properties.reStartTime)/1000000);
			return readyMsg;
		}

		if (requestMsg.contains("SNAP")){
			Properties datamap = properties.getDataMap();
			datamap.clear();
			LOG.debug("SNAP message received = " + requestMsg);
			String[] decodedSnap = requestMsg.split(":");
			LOG.debug("Snap decoded is = " + decodedSnap[0] + "	" + decodedSnap[1]);
			String[] snap = decodedSnap[1].replaceAll("\\{", "").replaceAll("\\}", "").split(", ");
			for (int i = 0; i < snap.length; i++){
				snap[i] = snap[i].trim();
				if (snap[i].length() == 0) break;
				LOG.debug("Snap i = " + i + " Snap[i] = " + snap[i]);
				String[] datamapEntryArr = snap[i].split("=");

				String key = datamapEntryArr[0].trim();
				String value = datamapEntryArr[1].trim();
				LOG.debug("Key is = " + key);
				LOG.debug("Value is = " + value);
				datamap.put(key, value);
			}
			FileOps.writeDataMap(properties);
			LOG.debug("Updated datamap = " + properties.getDataMap());
			LOG.debug("Follower ready for Broadcast");
			long reEndTime = System.nanoTime();
			LOG.info("Time required for Recovery Phase:" + (reEndTime-properties.reStartTime)/1000000);
			return "READY:" + this.properties.getNodeId();
		}

		if (requestMsg.contains("NEXT")){
			LOG.info("Received: " + requestMsg);
			long reEndTime = System.nanoTime();
			LOG.info("Time required for Recovery Phase:" + (reEndTime-properties.reStartTime)/1000000);
			return "READY:" + this.properties.getNodeId();
			
		}

		if (requestMsg.contains("READY")){
			
			
			properties.getSynData().incrementQuorumCount();
		}

		return "";
	
	}



	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
//		cause.printStackTrace();
		LOG.error("Context Name:"+ ctx.name());
		LOG.error("Context :"+ ctx.toString());
		LOG.error(cause.getMessage());
		ctx.close();
	}




}