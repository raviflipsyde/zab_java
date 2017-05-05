package servers;

import servers.NodeServerProperties1.State;

/**
 * @author rpatel16, pghanek
 *
 */
public class Notification{
	//this order is preserved from the paper. It is advisable to use the same order while initializing the member variables for consistency.
	private Vote vote;
	private long senderId;
	private State senderState;
	private long senderRound;
	
	public Vote getVote() {
		return this.vote;
	}
	public void setVote(Vote vote) {
		this.vote = vote;
	}
	public long getSenderRound() {
		return this.senderRound;
	}
	public void setSenderRound(long senderRound) {
		this.senderRound = senderRound;
	}
	public long getSenderId() {
		return this.senderId;
	}
	public void setSenderId(long senderId) {
		this.senderId = senderId;
	}
	public NodeServerProperties1.State getSenderState() {
		return this.senderState;
	}
	public void setSenderState(NodeServerProperties1.State state) {
		this.senderState = state;
	}
	
	public Notification(Vote vote, long id, servers.NodeServerProperties1.State state, long round) {
		//this order is preserved from the paper. It is advisable to use the same order while initializing the member variables for consistency.
		this.vote = vote;
		this.senderId = id;
		this.senderState = state;
		this.senderRound = round;
		
	}
	
	public Notification(String notification) {
		//Vote vote, long id, servers.NodeServerProperties1.State state, long round
		//TODO: serialization of incoming notification
		String[] arr = notification.split(",");
		long senderID = Long.parseLong(arr[0]); //sender of vote
		long voteNodeID = Long.parseLong(arr[1]); // NodeID being voted
		long voteZxIDEpoch = Long.parseLong(arr[2]); // ZxID - Epoch
		long voteZxIDCounter = Long.parseLong(arr[3]); //ZxID - Counter
		NodeServerProperties1.State SenderState = Enum.valueOf(State.class, arr[4]); // State of sender
		long senderRound = Long.parseLong(arr[5]); //Round of sender
		
		ZxId zxid1 = new ZxId(voteZxIDEpoch, voteZxIDCounter);
		Vote vote1 = new Vote(zxid1, voteNodeID);
		
		this.vote = vote1;
		this.senderRound = senderRound;
		this.senderId = senderID;
		this.senderRound = senderRound;
		this.senderState = SenderState;
		
	}
	
	
	@Override
	public String toString() {
		return this.senderId + "," + this.vote.getId() + "," + this.vote.getZxid().getEpoch() + "," + this.vote.getZxid().getCounter() + "," + this.senderState.toString() + ","  + this.senderRound; 
	}
	
	
	
	
	
	
	
	
}
