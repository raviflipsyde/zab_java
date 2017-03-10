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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

/**
 * Handles a server-side channel.
 */
public class InHandler2 extends SimpleChannelInboundHandler<ByteBuf> { // (1)
	private static final Logger LOG = LogManager.getLogger(InHandler2.class);
	ChannelFuture connectFuture;	

	
	@Override
	public void channelActive(ChannelHandlerContext ctx)
			throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.channel(NioSocketChannel.class) 
		.handler(
				new SimpleChannelInboundHandler<ByteBuf>() { 
					@Override
					protected void channelRead0(
							ChannelHandlerContext ctx,
							ByteBuf in) throws Exception {
					System.out.println("Reveived data");
					in.clear();
				}
				});
		bootstrap.group(ctx.channel().eventLoop());
		connectFuture = bootstrap.connect(
				new InetSocketAddress("www.manning.com", 80)); 
	}
	
	
	@Override
	public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {

		ByteBuf in =  msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Server Recieved : "+requestMsg);

		ctx.write(Unpooled.copiedBuffer("Hello to You!! \r\n", StandardCharsets.UTF_8));
		ctx.flush(); // (2)

		if (connectFuture.isDone()) {
			// do something with the data #10
			System.out.println("I m in done....");
		}


	}

	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}


}