package serverHandlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import servers.NodeServer;
import servers.NodeServerProperties;
import servers.Notification;
import servers.Vote;

/**
 * Handles a server-side channel.
 */
public class InHandler1 extends ChannelInboundHandlerAdapter { // (1)
	private static final Logger LOG = LogManager.getLogger(InHandler1.class);
	private List<InetSocketAddress> memberList;
	private NodeServer server;
	
	public InHandler1(NodeServer server) {
		this.server = server;
	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		
		
				
		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Channel Read:" + requestMsg);
		String response = handleClientRequest(requestMsg);
		
		
		ctx.write(Unpooled.copiedBuffer(response+"\r\n", StandardCharsets.UTF_8));
		ctx.flush(); // (2)
		

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
			server.addMemberToList(addr);
			
			LOG.info(server.getMemberListString());
			
			return "OK";
		}
		
		if(requestMsg.contains("NOTIFICATION:")){
			//add the ip:port to the group member list;
			
			
			String[] arr = requestMsg.split(":");
			
			Notification responseNotification = new Notification(arr[1]);
			NodeServerProperties serverProp = server.getProperties();
			if(serverProp.getNodestate() == NodeServerProperties.State.ELECTION){
				serverProp.getElectionQueue().add(responseNotification);
				if(responseNotification.getSenderState() == NodeServerProperties.State.ELECTION
						&& responseNotification.getSenderRound() < serverProp.getElectionRound()){
					Vote myVote = new Vote(serverProp.getLastZxId(), serverProp.getCurrentEpoch(), serverProp.getId());
				}
			}
						
			LOG.info(server.getMemberListString());
			
			return "OK";
		}
		
		if(requestMsg.contains("OK")){
			//add the ip:port to the group member list;
			
			
//			String[] arr = requestMsg.split(":");
//			
//			InetSocketAddress addr = new InetSocketAddress(arr[1].trim(), Integer.parseInt(arr[2].trim()));
//			server.addMemberToList(addr);
			LOG.info("Client rreceived OK!!");
			LOG.info(server.getMemberList());
			
			return "";
		}
		
		
		return "HMM...";
		
	}



	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

	


}