package servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import serverHandlers.InHandler1;
import serverHandlers.InHandler2;

public class NodeServer1 {

	private static final Logger LOG = LogManager.getLogger(NodeServer1.class);

	private String bootstrapHost;
	private int bootstrapPort;
	private int nodePort;
	private List<InetSocketAddress> memberList;
	private List<Channel> channelList;
	private String myIP;

	public NodeServer1(String bhost, int bport, int nport) {

		this.bootstrapHost = bhost;
		this.bootstrapPort = bport;
		this.nodePort = nport;
		this.memberList = new ArrayList<InetSocketAddress>();
		this.channelList = new ArrayList<Channel>();
		myIP = getMyIP();
	}

	public void run(){

		EventLoopGroup bossGroup = new NioEventLoopGroup(); 
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		try {
			bootstrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new InHandler2())
			.option(ChannelOption.SO_BACKLOG, 128)          
            .childOption(ChannelOption.SO_KEEPALIVE, true); 

			ChannelFuture future = bootstrap.bind(new InetSocketAddress(nodePort));

			future.addListener(new ChannelFutureListener() {

				public void operationComplete(ChannelFuture channelFuture) throws Exception {

					if (channelFuture.isSuccess()) {
						System.out.println("Server bound");
					} else {
						System.err.println("Bound attempt failed");
						channelFuture.cause().printStackTrace();
					}


				}
			});



		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}

	}



	private String getMyIP() {

		BufferedReader in = null;
		String ip = " ";
		try {
			URL whatismyip = new URL("http://ipv4bot.whatismyipaddress.com/");
			in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			ip = in.readLine();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ip;

	}

}
