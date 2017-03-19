package servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import serverHandlers.BootStrapServerHandler;
import serverHandlers.InHandler1;
import serverHandlers.InHandler2;
import serverHandlers.NodeInboundHandler;
import serverHandlers.NodeOutboundHandler;
import util.TcpClient;
import util.TcpClient1;
import util.TcpRequestHandler;
import util.TcpServer;

public class NodeServer implements Runnable{

	private static final Logger LOG = LogManager.getLogger(NodeServer.class);
	private String bootstrapHost;
	private int bootstrapPort;
	private int nodePort;
	private List<InetSocketAddress> memberList;

	private String myIP;

	public NodeServer(String bhost, int bport, int nport){
		this.bootstrapHost = bhost;
		this.bootstrapPort = bport;
		this.nodePort = nport;
		this.memberList = new ArrayList<InetSocketAddress>();
		myIP = getMyIP();
	}



	public void run() {

		System.out.println("in Node Server run");

		// send the address to bootstrap
		msgBootstrap();
		// get the memberlist
		// memberlist set after msgBootstrap call

		try {
			// Start the tcp serve to listen to incoming msgs
			//			Thread serverThread = new Thread(new TcpServer(nodePort));
//			Thread serverThread = new Thread(new NettyServer(nodePort));
//			serverThread.start();
			for(InetSocketAddress member: memberList){
				
				String	ret  = new TcpClient1(member.getHostName(), member.getPort()).sendMsg("JOIN_GROUP:"+myIP+":"+nodePort);
				LOG.info("tcp client recieved "+ ret);	
		

			}
			
			this.runServer(nodePort);

			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// connect with all the members
		// start phase 0



	}


	public void msgBootstrap(){
		Socket socket;
		try {
			socket = new Socket (bootstrapHost, bootstrapPort);

			PrintWriter out = new PrintWriter (socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader (new InputStreamReader(socket.getInputStream ()));
			//set self_ip:port to bootsstrap

			out.println ("set "+ myIP + ":"+nodePort);


			String memberList = in.readLine ();
			//process memberlist
			parseMemberList(memberList);

			out.close ();
			in.close();
			socket.close ();


		} catch (UnknownHostException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}




	private void parseMemberList(String memberList) {

		String[] list = memberList.split(",");
		System.out.println("Members");
		for(String s:list){
			String[] address = s.split(":");
			String ip = address[0];
			int port = Integer.parseInt(address[1]);
			if(myIP.equals(ip) && nodePort== port){}
			else{
				InetSocketAddress addr = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
				this.memberList.add(addr);
			}
			
		}

	}


	public String getMyIP(){
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
	
	
	private void runServer(int port){

        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new InHandler1(memberList));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)
            
            LOG.info("Netty server started on port:"+port);
            
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    
	}

}
