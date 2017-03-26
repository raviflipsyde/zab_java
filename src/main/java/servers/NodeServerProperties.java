package servers;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NodeServerProperties {
	public enum State {ELECTION,LEADING,FOLLOWING };
	private long id;
	private long lastEpoch;
	private long currentEpoch;
	private long lastZxId;
	private boolean isLeader;
	private State nodestate;

	private Queue<String> electionQueue;
	private List<Message> messageList;

	
	public NodeServerProperties() {
		id = 0;
		lastEpoch = 0;
		currentEpoch = 0;
		lastZxId = 0;
		isLeader = false;
		nodestate = State.ELECTION;

		Queue<String> electionQueue = new ConcurrentLinkedQueue<String>();
		ArrayList<Message> messageList = new ArrayList<Message>();
	
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


	public synchronized Queue<String> getElectionQueue() {
		return electionQueue;
	}


	public synchronized void setElectionQueue(Queue<String> electionQueue) {
		this.electionQueue = electionQueue;
	}


	public synchronized List<Message> getMessageList() {
		return messageList;
	}


	public synchronized void setMessageList(List<Message> messageList) {
		this.messageList = messageList;
	}
	
	
}


