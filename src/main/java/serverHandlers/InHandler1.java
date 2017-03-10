package serverHandlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
		

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Server Recieved : "+requestMsg);
		
		ctx.write(Unpooled.copiedBuffer("Hello to You!! \r\n", StandardCharsets.UTF_8));
		ctx.flush(); // (2)

	}

	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}

	


}