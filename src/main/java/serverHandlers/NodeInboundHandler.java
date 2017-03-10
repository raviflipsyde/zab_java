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
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

public class NodeInboundHandler extends ChannelOutboundHandlerAdapter {
	private static final Logger LOG = LogManager.getLogger(NodeInboundHandler.class);
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		// TODO Auto-generated method stub
		super.write(ctx, msg, promise);
		LOG.info("in my write");
	}
	
	
//	@Override
//	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//		
////		System.out.println(msg);
////		
////		ctx.writeAndFlush(msg); // recieved message sent back directly
//
//		ByteBuf in = (ByteBuf) msg;
//		String response  =in.toString(StandardCharsets.UTF_8 );
//		LOG.info("Server Recieved : "+response);
//		
//	}
//
//	@Override
//	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		super.channelActive(ctx);
//		System.out.println("channelActive in NodeInboundHandler");
//		ctx.write(Unpooled.copiedBuffer("Hello", StandardCharsets.UTF_8));
//		ctx.flush();
//
//	}
//
//	@Override
//	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//		// Close the connection when an exception is raised.
//		System.out.println("Exception in client");
//		cause.printStackTrace();
//		ctx.close();
//	}
//	
//	@Override
//	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//		// TODO Auto-generated method stub
//		super.channelRegistered(ctx);
//		System.out.println("channelRegisted in NodeInboundHandler");
//		ctx.write(Unpooled.copiedBuffer("Hello", StandardCharsets.UTF_8));
//		ctx.flush();
//	}

}
