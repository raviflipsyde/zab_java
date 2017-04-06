package netty;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class ServerHandler3 extends ChannelOutboundHandlerAdapter{
	private static final Logger LOG = LogManager.getLogger(ServerHandler3.class);
	
	
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
    	LOG.info("Exception in Channel");
        cause.printStackTrace();
        ctx.close();
    }
    

}
