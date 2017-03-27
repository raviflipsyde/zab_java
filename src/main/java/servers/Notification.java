package servers;

import servers.NodeServerProperties.State;

public class Notification{
	
	private Vote vote;
	private long senderRound;
	private long senderId;
	private NodeServerProperties.State senderState;
	
	public Vote getVote() {
		return vote;
	}
	public void setVote(Vote vote) {
		this.vote = vote;
	}
	public long getSenderRound() {
		return senderRound;
	}
	public void setSenderRound(long senderRound) {
		this.senderRound = senderRound;
	}
	public long getSenderId() {
		return senderId;
	}
	public void setSenderId(long senderId) {
		this.senderId = senderId;
	}
	public NodeServerProperties.State getSenderState() {
		return senderState;
	}
	public void setSenderState(NodeServerProperties.State senderState) {
		this.senderState = senderState;
	}
	
	public Notification(Vote vote, long senderRound, long senderId, State senderState) {
		super();
		this.vote = vote;
		this.senderRound = senderRound;
		this.senderId = senderId;
		this.senderState = senderState;
	}
	
	
	public Notification(String notification) {
		String[] arr = notification.split(",");
		long voteId= Long.parseLong(arr[0]);
		long voteEpoch= Long.parseLong(arr[1]);
		long voteTxId= Long.parseLong(arr[2]);
		long senderId= Long.parseLong(arr[3]);
		long senderRound = Long.parseLong(arr[4]);
		
		String senderState = arr[5];
		System.out.println(senderState);
		
		
		Vote vote1 = new Vote(voteTxId, voteEpoch, voteId);
		this.vote = vote1;
		this.senderRound = senderRound;
		this.senderId = senderId;
		this.senderRound = senderRound;
		this.senderState = NodeServerProperties.State.valueOf(senderState);
		
	}
	
	public Notification(long voteid, long voteTxId, long voteEpoch, long senderRound, long senderId, State senderState) {
		super();
		this.vote = new Vote(voteTxId, voteEpoch, voteid);
		this.senderRound = senderRound;
		this.senderId = senderId;
		this.senderState = senderState;
	}
	
	@Override
	public String toString() {
		return "" + vote.getId()+ ","+ vote.getEpoch()+ ","+vote.getTxId() + "," +senderId+","+ senderRound + "," + senderState;
	}
	
	
	
	
	
	
	
	
}
