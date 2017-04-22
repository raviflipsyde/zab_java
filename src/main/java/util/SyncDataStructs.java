package util;

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import servers.Notification;
import servers.Proposal;
import servers.Vote;
import servers.ZxId;

public class SyncDataStructs {

	private static SyncDataStructs instance = null;
	private MpscArrayQueue<Notification> electionQueue = null;
	private List<InetSocketAddress> memberList = null;
	private ConcurrentHashMap<Long, Long> acceptedEpochMap = null;
	private ConcurrentHashMap<Long, ZxId> currentEpochMap = null;
	//ConcurrentHashMap<Long, InetSocketAddress> quorum = null;
	//private MpscArrayQueue<Proposal> proposeQueue = null; //Request Queue with each node
 	//private MpscArrayQueue<Proposal> commitQueue = null;
	
	//used during broadcast
	private volatile ConcurrentHashMap<Proposal, AtomicInteger> proposedTransactions = null; //<counter,num_ack> Map that the leader maintains to store acknowledgements of proposedtransactions
	private volatile SortedSet<Proposal> committedTransactions = null;
	
	private Vote myVote;
	private long newEpoch;
	
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
		proposedTransactions = new ConcurrentHashMap<Proposal, AtomicInteger>();
		committedTransactions = new TreeSet<Proposal>(comparator);
	 	
		//proposeQueue = new MpscArrayQueue<Proposal>(1000);
		//commitQueue = new MpscArrayQueue<Proposal>(1000);
		

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
	
	public synchronized ConcurrentHashMap<Proposal, AtomicInteger> getProposedTransactions() {
		return proposedTransactions;
	}
	
	public synchronized void setProposedTransactions(ConcurrentHashMap<Proposal, AtomicInteger> proposedTransactions) {
		this.proposedTransactions = proposedTransactions;
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
	public SortedSet<Proposal> getCommittedTransactions() {
		return committedTransactions;
	}
	public void setCommittedTransactions(SortedSet<Proposal> committedTransactions) {
		this.committedTransactions = committedTransactions;
	}
	
	
	
	
}
