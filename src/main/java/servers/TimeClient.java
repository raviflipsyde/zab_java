package servers;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import serverHandlers.TimeClientHandler;

public class TimeClient {
	private static final Logger LOG = LogManager.getLogger(TimeClient.class);
	ChannelFuture cf;
	Channel c;
	
	
	public TimeClient(Bootstrap b, EventLoopGroup workerGroup, String host, int port){
		b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new TimeClientHandler());
            }
        });
        
        this.cf = b.connect(host, port); // (5)
        
	}
	public ChannelFuture getChannelFuture(){
		return this.cf;			
	}
	
	public String writeMsg(String msg){
		
		ChannelFuture lastWriteFuture;
		
		try {
			
			Channel c = cf.sync().channel();
			
			lastWriteFuture = c.writeAndFlush(Unpooled.copiedBuffer(msg+"\r\n", StandardCharsets.UTF_8));
			
			lastWriteFuture.addListener(new ChannelFutureListener() {
				
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()){
						LOG.info("in listener1. op complete success");
						
					}
					else{
						future.cause().printStackTrace();
						future.channel().close();
					}
					
				}
			});
			
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return msg;
		
	}
//    public static void main(String[] args) throws Exception {
//        String host = args[0];
//        int port = Integer.parseInt(args[1]);
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//
//        try {
//            Bootstrap b = new Bootstrap(); // (1)
//            b.group(workerGroup); // (2)
//            b.channel(NioSocketChannel.class); // (3)
//            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
//            b.handler(new ChannelInitializer<SocketChannel>() {
//                @Override
//                public void initChannel(SocketChannel ch) throws Exception {
//                    ch.pipeline().addLast(new TimeClientHandler());
//                }
//            });
//
//            // Start the client.
//            ChannelFuture f = b.connect(host, port).sync(); // (5)
//            
//            // Wait until the connection is closed.
//            f.channel().closeFuture().sync();
//        } finally {
//            workerGroup.shutdownGracefully();
//        }
//    }
}