//package netty;
//
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import io.netty.bootstrap.ServerBootstrap;
//
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import netty.InHandler1;
//import servers.NodeServer;
//
//public class NettyServer implements Runnable {
//	private int port;
//	private NodeServer server;
//	private static final Logger LOG = LogManager.getLogger(NettyServer.class);
//    public NettyServer(int port, NodeServer server) {
//        this.port = port;
//        this.server = server;
//    }
//    
//    
//    public void run() {
//        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
//        EventLoopGroup workerGroup = new NioEventLoopGroup();
//        try {
//            ServerBootstrap b = new ServerBootstrap(); // (2)
//            b.group(bossGroup, workerGroup)
//             .channel(NioServerSocketChannel.class) // (3)
//             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
//                 @Override
//                 public void initChannel(SocketChannel ch) throws Exception {
//                     ch.pipeline().addLast(new InHandler1(server));
//                 }
//             })
//             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
//             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
//
//            // Bind and start to accept incoming connections.
//            ChannelFuture f = b.bind(port).sync(); // (7)
//            
//            LOG.debug("Netty server started on port:"+port);
//            
//            // Wait until the server socket is closed.
//            // In this example, this does not happen, but you can do that to gracefully
//            // shut down your server.
//            f.channel().closeFuture().sync();
//        } catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//            workerGroup.shutdownGracefully();
//            bossGroup.shutdownGracefully();
//        }
//    }
//
//}
