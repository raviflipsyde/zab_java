package servers;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import util.SyncDataStructs;

public class NodeServerProperties1 {
	public static enum State {
		ELECTION, LEADING, FOLLOWING
	};

	private String bootstrapHost;
	private int bootstrapPort;
	private String nodeHost;
	private int nodePort;

	private Map<Long, InetSocketAddress> memberList;


	private long nodeId;
	private long lastEpoch;
	private long acceptedEpoch;
	private long currentEpoch;
	private long newEpoch;
	private long counter;
	private ZxId lastZxId;
	private boolean isLeader;
	private State nodestate;
	private long electionRound;
	private long leaderId;
	private InetSocketAddress leaderAddress;
	private InetSocketAddress myAddress;

	private Queue<Message> requestQueue;
	private Queue<Message> commitQueue;
	private SyncDataStructs synData;
	private MpscArrayQueue<Notification> electionQueue;
	private Vote myVote;
	
	private Map<String, String> DataMap;

	public NodeServerProperties1() {
		//TODO: acceptedEpoch = 0;
		nodeId = 0;
		lastEpoch = 0;
		currentEpoch = 0;
		lastZxId = new ZxId(0, 0);
//		lastZxId.setEpoch(0);
//		lastZxId.setCounter(0);
		isLeader = false;
		electionRound = 0;
		nodestate = State.ELECTION;
		leaderId = 0;
		leaderAddress = null;
		requestQueue = new ConcurrentLinkedQueue<Message>();
		commitQueue = new ConcurrentLinkedQueue<Message>();
		electionQueue = new MpscArrayQueue<Notification>(100);
		nodeHost = getMyIP();
		myAddress = new InetSocketAddress(nodeHost, nodePort);
		// messageList = new ArrayList<Message>();
		myVote = new Vote(this.getLastZxId(), this.getNodeId());
		synData = SyncDataStructs.getInstance();
		memberList = new ConcurrentHashMap<Long, InetSocketAddress>();
		DataMap = new ConcurrentHashMap<String, String>();
	}

	public long getCounter() {
		return counter;
	}

	public void setCounter(long counter) {
		this.counter = counter;
	}

	public long getNewEpoch() {
		return newEpoch;
	}

	public void setNewEpoch(long newEpoch) {
		this.newEpoch = newEpoch;
	}

	public long getAcceptedEpoch() {
		return acceptedEpoch;
	}

	public void setAcceptedEpoch(long acceptedEpoch) {
		this.acceptedEpoch = acceptedEpoch;
	}

	public synchronized Vote getMyVote() {
		return myVote;
	}

	public synchronized void setMyVote(Vote myVote) {
		this.myVote = myVote;
	}

	public synchronized String getBootstrapHost() {
		return this.bootstrapHost;
	}

	public synchronized void setBootstrapHost(String bootstrapHost) {
		this.bootstrapHost = bootstrapHost;
	}

	public synchronized int getBootstrapPort() {
		return this.bootstrapPort;
	}

	public synchronized void setBootstrapPort(int bootstrapPort) {
		this.bootstrapPort = bootstrapPort;
	}

	public synchronized int getNodePort() {
		return this.nodePort;
	}

	public synchronized void setNodePort(int nodePort) {
		this.nodePort = nodePort;
	}

	
	public synchronized Map<String, String> getDataMap() {
		return DataMap;
	}

	public synchronized void setDataMap(Map<String, String> dataMap) {
		DataMap = dataMap;
	}

	public synchronized Map<Long, InetSocketAddress> getMemberList() {
		return memberList;
	}

	public synchronized void setMemberList(Map<Long, InetSocketAddress> memberList) {
		this.memberList = memberList;
	}

	public synchronized long getNodeId() {
		return this.nodeId;
	}

	public synchronized void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	public synchronized long getLastEpoch() {
		return this.lastEpoch;
	}

	public synchronized void setLastEpoch(long lastEpoch) {
		this.lastEpoch = lastEpoch;
	}

	public synchronized long getCurrentEpoch() {
		return currentEpoch;
	}

	public synchronized void setCurrentEpoch(long currentEpoch) {
		this.currentEpoch = currentEpoch;
	}

	public synchronized ZxId getLastZxId() {
		return lastZxId;
	}

	public synchronized void setLastZxId(ZxId lastZxId) {
		this.lastZxId = lastZxId;
	}

	
	public synchronized boolean isLeader() {
		return isLeader;
	}

	public synchronized void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}

	public synchronized State getNodestate() {
		return nodestate;
	}

	public synchronized void setNodestate(State nodestate) {
		this.nodestate = nodestate;
	}

	public synchronized long getElectionRound() {
		return electionRound;
	}

	public synchronized void setElectionRound(long electionRound) {
		this.electionRound = electionRound;
	}

	public synchronized long getLeaderId() {
		return leaderId;
	}

	public synchronized void setLeaderId(long leaderId) {
		this.leaderId = leaderId;
	}

	public synchronized InetSocketAddress getLeaderAddress() {
		return leaderAddress;
	}

	public synchronized void setLeaderAddress(InetSocketAddress leaderAddress) {
		this.leaderAddress = leaderAddress;
	}

	public synchronized Queue<Message> getRequestQueue() {
		return requestQueue;
	}

	public synchronized void setRequestQueue(Queue<Message> requestQueue) {
		this.requestQueue = requestQueue;
	}

	public synchronized Queue<Message> getCommitQueue() {
		return commitQueue;
	}

	public synchronized void setCommitQueue(Queue<Message> commitQueue) {
		this.commitQueue = commitQueue;
	}

	public synchronized String getNodeHost() {
		return nodeHost;
	}

	public synchronized void setNodeHost(String nodeHost) {
		this.nodeHost = nodeHost;
	}

	public synchronized InetSocketAddress getMyAddress() {
		return myAddress;
	}

	public synchronized void setMyAddress(InetSocketAddress myAddress) {
		this.myAddress = myAddress;
	}

	public synchronized SyncDataStructs getSynData() {
		return synData;
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
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ip;
	}

	public synchronized void addMemberToList(long nodeid, InetSocketAddress addr) {

		this.memberList.put(nodeid, addr);

	}

	public synchronized void removeMemberFromList(Long nodeId) {
		this.memberList.remove(nodeId);

	}

	public MpscArrayQueue<Notification> getElectionQueue() {
		return electionQueue;
	}

	public void setElectionQueue(MpscArrayQueue<Notification> electionQueue) {
		this.electionQueue = electionQueue;
	}

}
