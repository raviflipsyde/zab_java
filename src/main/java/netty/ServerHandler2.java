package netty;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import util.SyncDataStructs;

public class ServerHandler2 extends ChannelInboundHandlerAdapter{
	private static final Logger LOG = LogManager.getLogger(ServerHandler2.class);
	private SyncDataStructs sds = SyncDataStructs.getInstance();
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
		
		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Channel Read:" + requestMsg);
		ReferenceCountUtil.release(msg);	
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelActive(ctx);
		LOG.info("Channel Active");
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelRegistered(ctx);
		LOG.info("Channel Registered");
	}
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelUnregistered(ctx);
		LOG.info("Channel UnRegistered");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelInactive(ctx);
		LOG.info("Channel Inactive");
	}
	
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelReadComplete(ctx);
		LOG.info("Channel ReadComplete");
	}
	
	
	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelWritabilityChanged(ctx);
		LOG.info("Channel Writability Changed");
		
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
    	LOG.info("Exception in Channel");
        cause.printStackTrace();
        ctx.close();
    }
    

}
