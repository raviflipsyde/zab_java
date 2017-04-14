package util;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import servers.Message;
import servers.Notification;
import servers.Vote;

public class SyncDataStructs {

	private static SyncDataStructs instance = null;
	private MpscArrayQueue<Notification> electionQueue = null;
	private List<InetSocketAddress> memberList = null;
	private List<Long> acceptedEpochList = null;
	private Vote myVote;
	private long newEpoch;
	
	private SyncDataStructs(){
		electionQueue = new MpscArrayQueue<Notification>(100);
		memberList  = new CopyOnWriteArrayList<InetSocketAddress>();
		acceptedEpochList = new CopyOnWriteArrayList<Long>();

	}
	public static SyncDataStructs getInstance(){
		if(instance == null){
			instance = new SyncDataStructs();
		}
		return instance;
	}

	public long getNewEpoch() {
		return newEpoch;
	}

	public void setNewEpoch(long newEpoch) {
		this.newEpoch = newEpoch;
	}

	public List<Long> getAcceptedEpochList() {
		return acceptedEpochList;
	}

	public void setAcceptedEpochList(List<Long> acceptedEpochList) {
		this.acceptedEpochList = acceptedEpochList;
	}

	public MpscArrayQueue<Notification> getElectionQueue() {
		return getInstance().electionQueue;
	}
	
	public List<InetSocketAddress> getMemberList() {
		return getInstance().memberList;
	}
	
	public synchronized Vote getMyVote() {
		return myVote;
	}
	public synchronized void setMyVote(Vote myVote) {
		this.myVote = myVote;
	}
	
	
	
	
}
