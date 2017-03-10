package serverHandlers;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeOutboundHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOG = LogManager.getLogger(NodeOutboundHandler.class);
	
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		System.out.println("channelActive in NodeInboundHandler");
		ctx.write(Unpooled.copiedBuffer("Hello", StandardCharsets.UTF_8));
		ctx.flush();

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Close the connection when an exception is raised.
		System.out.println("Exception in client");
		cause.printStackTrace();
		ctx.close();
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelReadComplete(ctx);
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO Auto-generated method stub
	//	super.channelRead(ctx, msg);
		
		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Server Recieved in NodeOutboundHandler: "+requestMsg);
		System.out.println(requestMsg);
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelRegistered(ctx);
		System.out.println("channelRegisted in NodeOutboundHandler");
		ctx.write(Unpooled.copiedBuffer("Hello there", StandardCharsets.UTF_8));
		ctx.flush();
	}

}
