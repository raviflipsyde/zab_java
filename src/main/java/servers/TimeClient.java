package servers;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import serverHandlers.TimeClientHandler;

public class TimeClient {
	public ChannelFuture getChannelFuture(Bootstrap b, EventLoopGroup workerGroup, String host, int port){
		
		ChannelFuture f = null;
		try {
			b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new TimeClientHandler());
                }
            });

            // Start the client.
            f = b.connect(host, port).sync(); // (5)
            
            // Wait until the connection is closed.
            //f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
          //  workerGroup.shutdownGracefully();
			
        }
		return f;
		
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