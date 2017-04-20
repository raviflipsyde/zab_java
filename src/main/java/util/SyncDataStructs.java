package util;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import servers.Notification;
import servers.Vote;
import servers.ZxId;

public class SyncDataStructs {

	private static SyncDataStructs instance = null;
	private MpscArrayQueue<Notification> electionQueue = null;
	private List<InetSocketAddress> memberList = null;
	private ConcurrentHashMap<Long, Long> acceptedEpochMap = null;
	private ConcurrentHashMap<Long, ZxId> currentEpochMap = null;
	private Vote myVote;
	private long newEpoch;
	private boolean newEpochFlag;
	private AtomicInteger quorumCount;
	
	private SyncDataStructs(){
		electionQueue = new MpscArrayQueue<Notification>(100);
		memberList  = new CopyOnWriteArrayList<InetSocketAddress>();
		acceptedEpochMap = new ConcurrentHashMap<Long, Long>();
		currentEpochMap = new ConcurrentHashMap<Long, ZxId>();
		newEpochFlag = false;

	}
	public static SyncDataStructs getInstance(){
		if(instance == null){
			instance = new SyncDataStructs();
		}
		return instance;
	}

	public AtomicInteger getQuorumCount() {
		return quorumCount;
	}

	public synchronized void incrementQuorumCount(){
		quorumCount.getAndIncrement();
	}

	public void setQuorumCount(AtomicInteger quorumCounter) {
		this.quorumCount = quorumCounter;
	}

	public boolean isNewEpochFlag() {
		return newEpochFlag;
	}

	public void setNewEpochFlag(boolean newEpochFlag) {
		this.newEpochFlag = newEpochFlag;
	}

	public long getNewEpoch() {
		return newEpoch;
	}

	public void setNewEpoch(long newEpoch) {
		this.newEpoch = newEpoch;
	}

	public ConcurrentHashMap<Long, Long> getAcceptedEpochMap() {
		return acceptedEpochMap;
	}

//	public void setAcceptedEpochMap(ConcurrentHashMap<Long, Long> acceptedEpochMap) {
//		this.acceptedEpochMap = acceptedEpochMap;
//	}

	public ConcurrentHashMap<Long, ZxId> getCurrentEpochMap() {
		return currentEpochMap;
	}

//	public void setCurrentEpochMap(ConcurrentHashMap<Long, ZxId> currentEpochMap) {
//		this.currentEpochMap = currentEpochMap;
//	}

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
