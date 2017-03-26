package servers;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NodeServerProperties {
	public enum State {ELECTION,LEADING,FOLLOWING };
	private long id;
	private long lastEpoch;
	private long currentEpoch;
	private long lastZxId;
	private boolean isLeader;
	private State nodestate;
	private long electionRound;
	
	private Queue<Notification> electionQueue;
	private List<Message> messageList;

	
	public NodeServerProperties() {
		id = 0;
		lastEpoch = 0;
		currentEpoch = 0;
		lastZxId = 0;
		isLeader = false;
		electionRound = 1;
		nodestate = State.ELECTION;

		electionQueue = new ConcurrentLinkedQueue<Notification>();
		messageList = new ArrayList<Message>();
	
	}


	public synchronized long getId() {
		return id;
	}


	public synchronized void setId(long id) {
		this.id = id;
	}


	public synchronized long getLastEpoch() {
		return lastEpoch;
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


	public synchronized long getLastZxId() {
		return lastZxId;
	}


	public synchronized void setLastZxId(long lastZxId) {
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





	public Queue<Notification> getElectionQueue() {
		return electionQueue;
	}


	public void setElectionQueue(Queue<Notification> electionQueue) {
		this.electionQueue = electionQueue;
	}


	public synchronized List<Message> getMessageList() {
		return messageList;
	}


	public synchronized void setMessageList(List<Message> messageList) {
		this.messageList = messageList;
	}


	public long getElectionRound() {
		return electionRound;
	}


	public void setElectionRound(long electionRound) {
		this.electionRound = electionRound;
	}
	
	
}


