package netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

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

import servers.NodeServerProperties1;

/**
 * @author rpatel16
 *
 */
public class NettyClient1 {
	private static final Logger LOG = LogManager.getLogger(NettyClient1.class);
	// private SyncDataStructs sds = SyncDataStructs.getInstance();
	private static Bootstrap b = null;
	private NodeServerProperties1 properties;
	public NettyClient1( NodeServerProperties1 properties1) {
		this.properties = properties1;
		
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			b = new Bootstrap(); // (1)
			b.group(workerGroup); // (2)
			b.channel(NioSocketChannel.class); // (3)
			b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new InHandler2(properties));
				}
			});

			// Start the client.
			// ChannelFuture f = b.connect("localhost", 9000).sync(); // (5)

			// Wait until the connection is closed.
			// f.channel().closeFuture().sync();

			// SocketAddress remoteAddress = new InetSocketAddress("localhost",
			// 9000);
			//
			// ChannelFuture f = b.connect(remoteAddress).sync();
			// Channel ch = f.channel();
			// ChannelFuture lastWriteFuture =
			// ch.writeAndFlush(Unpooled.copiedBuffer("Hello World...123\r\n",
			// StandardCharsets.UTF_8));
			//
			// remoteAddress = new InetSocketAddress("localhost", 9001);
			// f = b.connect(remoteAddress).sync();
			// ch = f.channel();
			// lastWriteFuture = ch.writeAndFlush(Unpooled.copiedBuffer("Hello
			// World...123\r\n", StandardCharsets.UTF_8));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// workerGroup.shutdownGracefully();
		}

	}

	public void init() {

	}

	public void sendMessage(String ip, int port, String msg) {
		try {
			SocketAddress remoteAddress = new InetSocketAddress(ip, port);

			ChannelFuture f = b.connect(remoteAddress).sync();
			final Channel ch = f.channel();
			ChannelFuture lastWriteFuture = ch
					.writeAndFlush(Unpooled.copiedBuffer(msg + "\r\n", StandardCharsets.UTF_8));

			lastWriteFuture.channel().closeFuture().sync();
			ch.closeFuture().sync();
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
			LOG.debug(e.getMessage());
			LOG.info(""+ip+":"+port+ " is unreachable. Cannot send: "+ msg);
			LOG.debug("Remove this ip from the memberlist");
			long removeId = -1;
			for( Entry<Long, InetSocketAddress> entry : properties.getMemberList().entrySet()){
				String memberip = entry.getValue().getHostName();
				String memberString = entry.getValue().getHostString();
				int memberport = entry.getValue().getPort();
				if(port == memberport && memberip.equals(ip)){
					removeId = entry.getKey();
					break;
				}
			}
			LOG.debug("------------------**Remove "+removeId+" from memberlist");
			properties.removeMemberFromList(removeId);
			
		}

	}

	// public static void main(String[] args){
	// NettyClient nc = new NettyClient();
	//
	// nc.sendMessage("localhost", 9000, "Hello World");
	// nc.sendMessage("localhost", 9001, "Hello World1212");
	// nc.sendMessage("localhost", 9000, "Hello World123123");
	// }

}
