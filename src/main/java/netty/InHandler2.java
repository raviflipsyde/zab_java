package netty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

import servers.NodeServerProperties1;
import servers.Notification;
import servers.Vote;

/**
 * Handles a server-side channel.
 */
public class InHandler2 extends ChannelInboundHandlerAdapter { // (1)
	private static final Logger LOG = LogManager.getLogger(InHandler2.class);
	
	private NodeServerProperties1 properties;

	public InHandler2(NodeServerProperties1 nsProperties) {
		this.properties = nsProperties;
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {



		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Channel Read:" + requestMsg);
		String response = handleClientRequest(requestMsg);
		LOG.info("response:" + response);
		if(response.length()>0){
			ctx.write(Unpooled.copiedBuffer(response+"\r\n", StandardCharsets.UTF_8));
			ctx.flush(); // (2)
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

			InetSocketAddress addr = new InetSocketAddress(arr[1].trim(), Integer.parseInt(arr[2].trim()));
			properties.addMemberToList(addr);

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
				currentElectionQueue.offer(responseNotification);
				synchronized (currentElectionQueue) {
					currentElectionQueue.notify();
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				LOG.info("After:"+currentElectionQueue.currentProducerIndex());

				if(responseNotification.getSenderState() == NodeServerProperties1.State.ELECTION
						&& responseNotification.getSenderRound() < properties.getElectionRound()){

					// get my current vote from FLE or when FLE is underway
					Vote myVote = properties.getSynData().getMyVote();
					Notification myNotification = new Notification(myVote, properties.getElectionRound(), properties.getNodeId(), properties.getNodestate());
					return("SNOTIFICATION:"+myNotification.toString());

				}
			}
			else{
				if(responseNotification.getSenderState() == NodeServerProperties1.State.ELECTION){
					// get my current vote from FLE or when FLE is underway
					Vote myVote = properties.getSynData().getMyVote();

					Notification myNotification = new Notification(myVote, properties.getElectionRound(), properties.getNodeId(), properties.getNodestate());
					LOG.info("myNotification:"+myNotification);
					return("SNOTIFICATION:"+myNotification.toString());

				}

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
				synchronized (currentElectionQueue) {
					currentElectionQueue.notify();
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
			LOG.info("Client rreceived OK!!");
			LOG.info(properties.getMemberList());

			return "";
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