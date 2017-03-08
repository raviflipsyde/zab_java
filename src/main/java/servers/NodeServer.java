package servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import serverHandlers.ClientHandler;

public class NodeServer implements Runnable{
	private String host;
	private int port;

	public NodeServer(String host, int port){
		this.host = host;
		this.port = port;
	}



	public void run() {
		System.out.println("in run");

		// send the address to bootstrap
		// get the memberlist
		// start phase 0
		EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.remoteAddress(new InetSocketAddress(host, port));		    
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new ClientHandler());
				}
			});

			ChannelFuture f = b.connect().sync();; 
			f.channel().closeFuture().sync();


		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}


	}


	public void msgBootstrap(){
		Socket socket;
		try {
			socket = new Socket (host, port);
			
			PrintWriter out = new PrintWriter (socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader (new InputStreamReader(socket.getInputStream ()));
			//set self_ip:port to bootsstrap
			out.println ("set 192.168.1.20:2020");
		
			
			String memberList = in.readLine ();
			//process memberlist
			System.out.println ("Relaying: " + memberList);
			out.close ();
			in.close();
			socket.close ();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void relatBootstrap(){
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket (1234);

	     Socket rxSocket = serverSocket.accept ();
	     BufferedReader in = new BufferedReader (new InputStreamReader(rxSocket.getInputStream ()));
	    
	     String fromClient = in.readLine ();
	                                   
	     in.close ();        
	     rxSocket.close ();
	     serverSocket.close ();
	 
	     System.out.println ("Relaying: " + fromClient);
	    
	     Socket txSocket = new Socket ("127.0.0.1", 2345);
	     PrintWriter out = new PrintWriter (txSocket.getOutputStream(), true);
	    
	     out.println (fromClient);
	    
	     out.close ();
	     txSocket.close ();
	     
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
}
