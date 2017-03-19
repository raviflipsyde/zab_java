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

/**
 * Handles a server-side channel.
 */
public class InHandler1 extends ChannelInboundHandlerAdapter { // (1)
	private static final Logger LOG = LogManager.getLogger(InHandler1.class);
	private List<InetSocketAddress> memberList;

	public InHandler1(List<InetSocketAddress> memberList1) {
		this.memberList = memberList1;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Server Recieved : "+requestMsg);
		String response = handleClientRequest(requestMsg);
		LOG.info("handleClientRequest sentback:"+response);
		
		ctx.write(Unpooled.copiedBuffer(response+"\r\n", StandardCharsets.UTF_8));
		ctx.flush(); // (2)
		

	}

	@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
			
			super.channelRegistered(ctx);
			LOG.info("Channel Registered: "+ctx.channel().localAddress());
		}
	

	private String handleClientRequest(String requestMsg) {
//		LOG.info("handleClientRequest:"+requestMsg);
		
		if(requestMsg.contains("JOIN_GROUP:")){
			//add the ip:port to the group member list;
			
			LOG.info("Server Recieved : "+requestMsg);
			String[] arr = requestMsg.split(":");
			LOG.info("InetAddress:"+arr[1]+"-"+arr[2]);
//			InetSocketAddress addr = new InetSocketAddress(arr[1], Integer.parseInt(arr[2]));
//			memberList.add(addr);
			return "OK";
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