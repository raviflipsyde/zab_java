package netty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
		LOG.info("Channel Registered: "+ctx.channel().localAddress() + ":" + ctx.channel().remoteAddress());
	}


	private String handleClientRequest(String requestMsg) {
		//		LOG.info("handleClientRequest:"+requestMsg);

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
			//add the ip:port to the group member list;
			

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
			LOG.info("Request msg is = " + requestMsg);
			String[] accEpoch = requestMsg.split(":");
			long nodeId = Long.parseLong(accEpoch[1].trim());
			long acceptedEpoch = Long.parseLong(accEpoch[2].trim());
//			long currentEpoch = Long.parseLong(accEpoch[3].trim());
//			long currentCounter = Long.parseLong(accEpoch[4].trim());

			//ZxId followerLastCommittedZxid = new ZxId(currentEpoch, currentCounter);

			ConcurrentHashMap<Long, Long> acceptedEpochMap = this.properties.getSynData().getAcceptedEpochMap();
			//ConcurrentHashMap<Long, ZxId> currentEpochMap = properties.getSynData().getCurrentEpochMap();

			acceptedEpochMap.put(nodeId, acceptedEpoch);
			//currentEpochMap.put(nodeId, followerLastCommittedZxid);

			if (acceptedEpochMap.size() < (this.properties.getMemberList().size() / 2) ){
				synchronized (acceptedEpochMap){
					try {
						acceptedEpochMap.wait();
						Thread.sleep(10);
					} catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}

			while (!this.properties.getSynData().isNewEpochFlag()){
				try {
					Thread.sleep(10);
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}

			return "NEWEPOCH:" + properties.getNewEpoch() + ":" + this.properties.getNodeId();
		}

		if (requestMsg.contains("NEWEPOCH")){

			Map<Long, InetSocketAddress> memberList = this.properties.getMemberList();
			LOG.info("Member List is = " + memberList);

			String ackepochmsg = "";
			String[] newEpocharr = requestMsg.split(":");
			long newEpoch = Long.parseLong(newEpocharr[1].trim());
			long nodeId = Long.parseLong(newEpocharr[2].trim());
			properties.getSynData().setNewEpoch(newEpoch);
			LOG.info("New Epoch received is = " + newEpoch);
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

				this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(), memberList.get(nodeId).getPort(), ackepochmsg);

				return "";

			} else {

				this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
				LOG.info("Going to Leader Election");

			}

		}

		if (requestMsg.contains("ACKEPOCH")){
			LOG.info("Ack new epoch message received = " + requestMsg);
			Map<Long, InetSocketAddress> memberList = this.properties.getMemberList();
			String[] currEpochArr = requestMsg.split(":");
			long nodeId = Long.parseLong(currEpochArr[1].trim());
			long currentEpoch = Long.parseLong(currEpochArr[2].trim());
			long currentCounter = Long.parseLong(currEpochArr[3].trim());

			ZxId followerLastCommittedZxid = new ZxId(currentEpoch, currentCounter);
			LOG.info("follower last committed zxid = " + followerLastCommittedZxid.getEpoch()
					+ "	" + followerLastCommittedZxid.getCounter());

//			ConcurrentHashMap<Long, ZxId> currentEpochMap = properties.getSynData().getCurrentEpochMap();
//			currentEpochMap.put(nodeId, followerLastCommittedZxid);

			String leaderLastLog = FileOps.readLastLog(properties);
			LOG.info("Leader last log = " + leaderLastLog);
			String[] arr1 = leaderLastLog.split(",");
			long epoch = Long.parseLong(arr1[0].trim());
			long counter = Long.parseLong(arr1[1].trim());

			ZxId leaderLastCommittedZxid = new ZxId(epoch, counter);
			LOG.info("leader last committed zxid = " + leaderLastCommittedZxid.getEpoch()
					+ "	" + leaderLastCommittedZxid.getCounter());

			if (leaderLastCommittedZxid.getEpoch() == followerLastCommittedZxid.getEpoch()){

				if (followerLastCommittedZxid.getCounter() < leaderLastCommittedZxid.getCounter()){

					// TODO: Send DIFF message

					this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(), memberList.get(nodeId).getPort(), "DIFF");


					// TODO: Iterate through CommitHistory (refer readHistory()), stringify and send

				} else if (followerLastCommittedZxid.getCounter() == leaderLastCommittedZxid.getCounter()){
					// Do nothing
				} else if (followerLastCommittedZxid.getCounter() > leaderLastCommittedZxid.getCounter()){
					// Go to Leader Election. Ideally, shouldn't happen
					this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
					LOG.info("Going to Leader Election");
					// change phase
				}

			} else if (followerLastCommittedZxid.getEpoch() < leaderLastCommittedZxid.getEpoch()){

				// TODO: Send SNAP message
				this.nettyClientInhandler.sendMessage(memberList.get(nodeId).getHostName(), memberList.get(nodeId).getPort(), "SNAP");

				// TODO: Iterate through the Map, stringify each entry and then send

			} else if (followerLastCommittedZxid.getEpoch() > leaderLastCommittedZxid.getEpoch()){
				// Go to Leader Election. Ideally, shouldn't happen
				this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
				LOG.info("Going to Leader Election");
				//changePhase();
			}

			return "";

		}

		if (requestMsg.contains("DIFF")){
			LOG.info("DIFF message received");
			LOG.info("Follower ready for Broadcast");
			return "ACKACTION:" + this.properties.getNodeId();
		}

		if (requestMsg.contains("SNAP")){
			LOG.info("SNAP message received");
			LOG.info("Follower ready for Broadcast");
			return "ACKACTION:" + this.properties.getNodeId();
		}

		if (requestMsg.contains("ACKACTION")){

			properties.getSynData().incrementQuorumCount();
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