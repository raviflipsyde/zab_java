package netty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

import servers.NodeServerProperties1;
import servers.Notification;
import servers.Proposal;
import servers.Vote;
import servers.ZxId;
import util.FileOps;

/**
 * Handles a server-side channel.
 */
public class InHandler2 extends ChannelInboundHandlerAdapter { // (1)
	private static final Logger LOG = LogManager.getLogger(InHandler2.class);
	
	private NodeServerProperties1 properties;
	private NettyClient1 nettyClient;
	public InHandler2(NodeServerProperties1 nsProperties) {
		this.properties = nsProperties;
		nettyClient = new NettyClient1(nsProperties);
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		ByteBuf in = (ByteBuf) msg;
		String requestMsg  = in.toString(StandardCharsets.UTF_8 );
		LOG.info(">>>Channel Read:" + requestMsg);
		String response = handleClientRequest(requestMsg);
		LOG.info("<<<Response:" + response);
		
		if(response.length()>0){
			ctx.write(Unpooled.copiedBuffer(response+"\r\n", StandardCharsets.UTF_8));
			ctx.flush(); // (2)
			ctx.close();
		}
		


	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

		super.channelRegistered(ctx);
		LOG.info("Channel Registered: "+ ctx.channel().localAddress() + ":" + ctx.channel().remoteAddress());
	}


	private String handleClientRequest(String requestMsg) {
		//		LOG.info("handleClientRequest:"+requestMsg);

		if(requestMsg.contains("WRITE:")){
			if(!properties.isLeader()){ //follower
				//Forward write request to the leader
				LOG.info("Follower received WRITE request from client, forwarding to the leader..!!");
				this.nettyClient.sendMessage(properties.getLeaderAddress().getHostName(), properties.getLeaderAddress().getPort(), requestMsg);
				return "OK";
				
			}
			else{ //leader

				//"WRITE:KEY:VALUE"
				String[] arr = requestMsg.split(":");
				
				//Key-value pair to be proposed
				String key = arr[1].trim();
				String value = arr[2].trim();
				long epoch = 3;//this.properties.getAcceptedEpoch()
				long counter = 4;//this.getCounter()++;
			
				
				//Form a proposal
				ZxId z = new ZxId(epoch, counter);
				Proposal p = new Proposal(z, key, value);
				String proposal = "PROPOSE:" + p.toString();
				
				//enqueue this proposal to proposed transactions to keep the count of Acknowledgements
				
				ConcurrentHashMap<Proposal, AtomicInteger> proposedtransactions = properties.getSynData().getProposedTransactions();
				proposedtransactions.put(p, new AtomicInteger(1));
				
				//checking if the entry is enqueued in the proposed transaction map
				LOG.info("Checking the counter right after enqueueing the entry: " + proposedtransactions.get(p));
				
				//send proposal to quorum
				LOG.info("Leader:" + "Sending proposal to everyone:" + proposal);
				
				LOG.info("Number of members:" + properties.getMemberList().size());
				
				for (Entry<Long, InetSocketAddress> member : properties.getMemberList().entrySet()) {
						LOG.info("Sending "+proposal+" to: "+ member.getValue().getHostName() + ":"+ member.getValue().getPort());
						this.nettyClient.sendMessage(member.getValue().getHostName(), member.getValue().getPort(), proposal);
				}

			}
		
			return "OK";
		}
		
		if(requestMsg.contains("PROPOSE")){
			if(properties.isLeader()){ // Leader will not accept this message
				LOG.info("I am the Leader, I do not accept proposals");
				return "ERROR: I am the eader, I send proposals, not accept!";
				
			}
			else{ ///Follower
				//enqueue this message to proposal queue 
				String[] arr = requestMsg.split(":");
				Long epoch = Long.parseLong(arr[1].trim());
				Long counter = Long.parseLong(arr[2].trim());
				ZxId z = new ZxId(epoch, counter);
				String key = arr[3].trim();
				String value = arr[4].trim();
				Proposal proposal = new Proposal(z,key,value);
				
				properties.getSynData().getProposedTransactions().put(proposal, new AtomicInteger(0));
				LOG.info("Enqueing proposal in Proposal Queue:" + proposal);
				
				LOG.info("Sending Acknowledgement to the leader");
				return "ACK_PROPOSAL:" + proposal.toString();
		
			}
		}
		
		if(requestMsg.contains("ACK_PROPOSAL")){
			if(!properties.isLeader()){//follower
				//follower should disregard this message
				LOG.info("Follower got ACK_PROPOSAL, shouldn't happen!");
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
				LOG.info("Leader: Got ACK_PROPOSAL, incrementing count for zxid" + z);
				
				//checking the ack count for the proposal (counter value)		
				ConcurrentHashMap<Proposal, AtomicInteger> proposedtransactions = properties.getSynData().getProposedTransactions();
				
				synchronized (proposedtransactions) {
					int count = proposedtransactions.get(p).incrementAndGet();
					proposedtransactions.put(p, new AtomicInteger(count));
				}
				//LOG.info("##################ACK count for proposal before incrementing####################" + proposedtransactions.get(p));
//				int count = proposedtransactions.get(p).incrementAndGet();
//				proposedtransactions.put(p, new AtomicInteger(count));
				//LOG.info("###################ACK count for proposal after incrementing####################" + proposedtransactions.get(p));
				
				return "OK";
			}
			
		}
		
		if(requestMsg.contains("COMMIT:")){
			if(properties.isLeader()){ // leader will not accept this message
				LOG.info("I am the Leader, I do not accept commit messages");
				return "ERROR: I am the eader, I send proposals, not accept!";
			}
			else{//follower
				LOG.info ("Follower received COMMIT message");
				LOG.info ("COMMIT message is:" + requestMsg);
				String[] arr = requestMsg.split(":");
				
				//Parsing proposal for which acknowledgement was received				
				Long epoch = Long.parseLong(arr[1].trim());
				Long counter = Long.parseLong(arr[2].trim());
				ZxId z = new ZxId(epoch, counter);
				String key = arr[3].trim();
				String value = arr[4].trim();
				Proposal p = new Proposal(z,key,value);
				ConcurrentHashMap<Proposal, AtomicInteger> proposalMap = properties.getSynData().getProposedTransactions();
				LOG.info("Map Size when Commit received: "+proposalMap.size());
				LOG.info("Map when Commit received: "+proposalMap);
				if(proposalMap.containsKey(p)){
					LOG.info("Commit Queue contains the transaction to be removed:" + p);
					//String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
					//FileOps.appendTransaction(fileName, p.toString());
					synchronized (properties.getSynData().getProposedTransactions()) {
						//remove from proposedtransactions map
						LOG.info("Inside synchronized block....!!!");
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

			LOG.info(properties.getMemberList());

			return "OK";
		}

		if(requestMsg.contains("CNOTIFICATION:")){
			//add the ip:port to thefore group member list;
			

			String[] arr = requestMsg.split(":");

			Notification responseNotification = new Notification(arr[1].trim());
			
			if(properties.getNodestate() == NodeServerProperties1.State.ELECTION){
				
				MpscArrayQueue<Notification> currentElectionQueue = properties.getSynData().getElectionQueue();
				LOG.info("Before:"+currentElectionQueue.currentProducerIndex());
				LOG.info("adding notification to the queue"+ responseNotification.toString());
				currentElectionQueue.offer(responseNotification);
				
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				LOG.info("After:"+currentElectionQueue.currentProducerIndex());
				LOG.info("NODE is in STATE: "+ properties.getNodestate());
				LOG.info("My Election ROUND: "+ properties.getElectionRound());
				LOG.info("his Election ROUND: "+ responseNotification.getSenderRound());
				
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
					LOG.info("myNotification:"+myNotification);
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
				LOG.info("Before:"+currentElectionQueue.currentProducerIndex());
				currentElectionQueue.offer(responseNotification);
				
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			
				LOG.info("After:"+currentElectionQueue.currentProducerIndex());


			}
			


			LOG.info(properties.getMemberList());
			return("ERROR");

		}
		
		if(requestMsg.contains("OK")){
			//add the ip:port to the group member list;


			//			String[] arr = requestMsg.split(":");
			//			
			//			InetSocketAddress addr = new InetSocketAddress(arr[1].trim(), Integer.parseInt(arr[2].trim()));
			//			server.addMemberToList(addr);
			LOG.info("Client received OK!!");
			LOG.info(properties.getMemberList());

			return "";
		}

		if (requestMsg.contains("FOLLOWERINFO")){
			String[] accEpoch = requestMsg.split(":");
			long nodeId = Long.parseLong(accEpoch[1]);
			long acceptedEpoch = Long.parseLong(accEpoch[2]);
			long currentEpoch = Long.parseLong(accEpoch[3]);
			long currentCounter = Long.parseLong(accEpoch[4]);

			ZxId followerLastCommittedZxid = new ZxId(currentEpoch, currentCounter);

			ConcurrentHashMap<Long, Long> acceptedEpochMap = properties.getSynData().getAcceptedEpochMap();
			ConcurrentHashMap<Long, ZxId> currentEpochMap = properties.getSynData().getCurrentEpochMap();
			acceptedEpochMap.put(nodeId, acceptedEpoch);
			currentEpochMap.put(nodeId, followerLastCommittedZxid);

			properties.getSynData().setAcceptedEpochMap(acceptedEpochMap);
			properties.getSynData().setCurrentEpochMap(currentEpochMap);

			return "";
		}

		if (requestMsg.contains("NEWEPOCH")){
			String[] newEpocharr = requestMsg.split(":");
			long newEpoch = Long.parseLong(newEpocharr[1]);
		}

		return "";
	
	}



	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}




}