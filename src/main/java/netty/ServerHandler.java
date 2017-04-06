package netty;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerHandler extends SimpleChannelInboundHandler<String>{
	private static final Logger LOG = LogManager.getLogger(ServerHandler.class);
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, String msg) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("Channel Read0:" + msg);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
//		super.channelActive(ctx);
		LOG.info("Channel Active");
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelReadComplete(ctx);
		LOG.info("Channel ReadComplete");
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
//		super.channelRegistered(ctx);
		LOG.info("Channel Registered");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext arg0, Object msg) throws Exception {
		// TODO Auto-generated method stub
		super.channelRead(arg0, msg);
		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Channel Read:" + requestMsg);
		
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelUnregistered(ctx);
		LOG.info("Channel UnRegistered");
	}
	
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOG.info("Exception in Channel");
		cause.printStackTrace();
        ctx.close();
    }
	

}
