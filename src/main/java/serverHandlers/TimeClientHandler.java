package serverHandlers;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TimeClientHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger LOG = LogManager.getLogger(TimeClientHandler.class);
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		
		String response = handleClientRequest(requestMsg);
		
		
		ctx.write(Unpooled.copiedBuffer(response+"\r\n", StandardCharsets.UTF_8));
		ctx.flush(); // (2)
    }

    private String handleClientRequest(String requestMsg) {

//		LOG.info("handleClientRequest:"+requestMsg);
		
		if(requestMsg.contains("OK")){
			//add the ip:port to the group member list;
			
			
//			String[] arr = requestMsg.split(":");
//			
//			InetSocketAddress addr = new InetSocketAddress(arr[1].trim(), Integer.parseInt(arr[2].trim()));
//			server.addMemberToList(addr);
			LOG.info("Client rreceived OK!!");
			return "";
		}
		
		
		return "...";
		
	
	}

	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}