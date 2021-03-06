package util;

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import servers.Notification;
import servers.Proposal;
import servers.Vote;
import servers.ZxId;

/**
 * @author rpatel16, pghanek, fvravani
 *
 */
public class SyncDataStructs {

	private static SyncDataStructs instance = null;
	private volatile MpscArrayQueue<Notification> electionQueue = null;
	private List<InetSocketAddress> memberList = null;
	private volatile ConcurrentHashMap<Long, Long> acceptedEpochMap = null;
	private volatile ConcurrentHashMap<Long, ZxId> currentEpochMap = null;
	private Vote myVote;
	private long newEpoch;
	private volatile boolean newEpochFlag;
	private AtomicInteger quorumCount;

	//used during broadcast
	private volatile SortedMap<Proposal, AtomicInteger> proposedTransactions = null; //<counter,num_ack> Map that the leader maintains to store acknowledgements of proposedtransactions
	private volatile SortedSet<Proposal> committedTransactions = null;
	


	public Comparator<Proposal> comparator = new Comparator<Proposal>() {
		public int compare(Proposal p1, Proposal p2) {
			if (p1.getZ().compareTo(p2.getZ()) == 1)
				return 1;
			else if (p1.getZ().compareTo(p2.getZ()) == -1)
				return -1;
			else
				return 0;
		}
	};

	private SyncDataStructs(){
		electionQueue = new MpscArrayQueue<Notification>(100);
		memberList  = new CopyOnWriteArrayList<InetSocketAddress>();
		acceptedEpochMap = new ConcurrentHashMap<Long, Long>();
		currentEpochMap = new ConcurrentHashMap<Long, ZxId>();

		newEpochFlag = false;
		proposedTransactions = new ConcurrentSkipListMap<Proposal, AtomicInteger>();
		committedTransactions = new ConcurrentSkipListSet<Proposal>(comparator);



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

	public void setAcceptedEpochMap(ConcurrentHashMap<Long, Long> acceptedEpochMap) {
		this.acceptedEpochMap = acceptedEpochMap;
	}

	public ConcurrentHashMap<Long, ZxId> getCurrentEpochMap() {
		return currentEpochMap;
	}

	public void setCurrentEpochMap(ConcurrentHashMap<Long, ZxId> currentEpochMap) {
		this.currentEpochMap = currentEpochMap;
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
//
//	public ConcurrentHashMap<Long, InetSocketAddress> getQuorum() {
//		return this.quorum;
//	}


	public SortedSet<Proposal> getCommittedTransactions() {
		return committedTransactions;
	}
	public synchronized SortedMap<Proposal, AtomicInteger> getProposedTransactions() {
		return proposedTransactions;
	}
	public synchronized void setProposedTransactions(SortedMap<Proposal, AtomicInteger> proposedTransactions) {
		this.proposedTransactions = proposedTransactions;
	}
	public void setCommittedTransactions(SortedSet<Proposal> committedTransactions) {
		this.committedTransactions = committedTransactions;
	}
	
	

	//	public MpscArrayQueue<Proposal> getProposeQueue() {

//		return proposeQueue;
//	}
//	public void setProposeQueue(MpscArrayQueue<Proposal> proposeQueue) {
//		this.proposeQueue = proposeQueue;
//	}
//	public MpscArrayQueue<Proposal> getCommitQueue() {
//		return commitQueue;
//	}
//	public void setCommitQueue(MpscArrayQueue<Proposal> commitQueue) {
//		this.commitQueue = commitQueue;
//	}
	

}

