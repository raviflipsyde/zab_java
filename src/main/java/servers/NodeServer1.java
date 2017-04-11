package servers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import netty.NettyClient1;
import netty.NettyServer1;
import util.UdpClient1;
import util.UdpServer1;

public class NodeServer1 {
	private static final Logger LOG = LogManager.getLogger(NodeServer1.class);
	private NodeServerProperties1 properties;

	public NodeServer1(NodeServerProperties1 properties) {
		super();
		this.properties = properties;
	}

	public NodeServer1(String bhost, int bport, int nport) {
		super();
		this.properties = new NodeServerProperties1();
		this.properties.setBootstrapHost(bhost);
		this.properties.setBootstrapPort(bport);
		this.properties.setNodePort(nport);
	}

	public void init() {
		LOG.info("Starting the Node server");

		msgBootstrap();

		LOG.info("ID for this node is :" + properties.getNodeId());

		Thread udpserverThread = new Thread(new UdpServer1(properties));
		udpserverThread.setPriority(Thread.MIN_PRIORITY);
		udpserverThread.start();

		Thread serverThread = new Thread(new NettyServer1(properties.getNodePort(), properties));
		serverThread.setPriority(Thread.MIN_PRIORITY);
		serverThread.start();

		Thread udpClientThread = new Thread(new UdpClient1(properties));
		udpClientThread.setPriority(Thread.MIN_PRIORITY);
		udpClientThread.start();

		NettyClient1 nc1 = new NettyClient1(properties);

		informGroupMembers(nc1);

		readHistory();

		changePhase();

	}


	private void changePhase() {
		try {
			Runnable target = new Runnable() {

				public void run() {
					// TODO Auto-generated method stub

				}
			};
			// TODO Auto-generated method stub
			Thread leaderElectionThread = new Thread(target);
			leaderElectionThread.start();
			leaderElectionThread.join();
			
			Thread recoveryThread = new Thread(target);
			recoveryThread.start();
			recoveryThread.join();
			
			Thread broadcastThread = new Thread(target);
			broadcastThread.start();
			broadcastThread.join();
		} 

		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long msgBootstrap() {
		Socket socket;
		long id = 0;
		try {
			socket = new Socket(properties.getBootstrapHost(), properties.getBootstrapPort());

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// set self_ip:port to bootsstrap

			out.println("set " + properties.getNodeHost() + ":" + properties.getNodePort());

			String memberList = in.readLine();
			String memberId = in.readLine();
			id = Long.parseLong(memberId);
			LOG.info("MemberID received:" + id);
			// process memberlist

			this.properties.setNodeId(id);

			parseMemberList(memberList);

			out.close();
			in.close();
			socket.close();

		} catch (UnknownHostException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		return id;
	}

	private void parseMemberList(String memberList) {

		String[] list = memberList.split(",");
		System.out.println("Members");
		for (String s : list) {
			String[] address = s.split(":");
			String ip = address[0];
			int port = Integer.parseInt(address[1]);
			if (properties.getNodeHost().equals(ip) && properties.getNodePort() == port) {
			} else {
				InetSocketAddress addr = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
				properties.addMemberToList(addr);

			}

		}

	}

	private void informGroupMembers(NettyClient1 nc) {

		List<InetSocketAddress> unreachablelist = new ArrayList<InetSocketAddress>();

		for (InetSocketAddress member : properties.getMemberList()) {

			try {
				nc.sendMessage(member.getHostName(), member.getPort(),
						"JOIN_GROUP:" + properties.getNodeHost() + ":" + properties.getNodePort());
			} catch (Exception e) {
				unreachablelist.add(member);
				e.printStackTrace();
			}

		}

		for (InetSocketAddress member : unreachablelist) {
			properties.removeMemberFromList(member);
		}

	}

	private void readHistory() {

		String fileName = "CommitedHistory_" + properties.getNodePort() + ".txt";
		String line = null;
		Queue<Message> msgList = properties.getCommitQueue();
		try {

			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				Message m = new Message(line);
				msgList.add(m);
				System.out.println(m);
			}

			bufferedReader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Message[] msgArr = new Message[msgList.size()];
		msgList.toArray(msgArr);

		Message lastMsg = msgArr[msgArr.length - 1];

		properties.setCurrentEpoch(lastMsg.getEpoch());
		properties.setLastZxId(lastMsg.getTxId());

	}

}
