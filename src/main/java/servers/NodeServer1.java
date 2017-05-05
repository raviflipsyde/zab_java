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
import java.util.*;
import java.util.Map.Entry;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import netty.NettyClient1;
import netty.NettyServer1;
import util.FileOps;
import util.UdpClient1;
import util.UdpServer1;

/**
 * @author rpatel16, pghanek, fvravani
 *
 */
public class NodeServer1 {
	private static final Logger LOG = LogManager.getLogger(NodeServer1.class);
	private NodeServerProperties1 properties;
	private Thread udpServerThread;
	private Thread nettyServerThread;
	private Thread udpClientThread;
	private NettyClient1 nettyClient;

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

	private Vote startLeaderElection() {

		LOG.info("Start Leader Election Phase");
		long leStartTime = System.nanoTime();
		properties.leStartTime = leStartTime;
		
		Map<Long, InetSocketAddress> memberList = this.properties.getMemberList();
		HashMap<Long, Vote> receivedVote = new HashMap<Long, Vote>();
		HashMap<Long, Long> receivedVotesRound = new HashMap<Long, Long>();
		HashMap<Long, Vote> OutOfElectionVotes = new HashMap<Long, Vote>();
		HashMap<Long, Long> OutOfElectionVotesRound = new HashMap<Long, Long>();
		MpscArrayQueue<Notification> currentElectionQueue = properties.getSynData().getElectionQueue();

		Vote myVote = new Vote(this.properties.getLastZxId(), this.properties.getNodeId());

		long limit_timeout = 15000;
		long timeout = 250;

		this.properties.setElectionRound(this.properties.getElectionRound() + 1);
		this.properties.setMyVote(myVote);

		Notification myNotification = new Notification(this.properties.getMyVote(), this.properties.getNodeId(),
				this.properties.getNodestate(), this.properties.getElectionRound());
		LOG.debug("My Notification is:" + myNotification.toString());

		// TODO: Verify
		sendNotificationToAll(myNotification.toString());

		while (this.properties.getNodestate() == NodeServerProperties1.State.ELECTION && timeout < limit_timeout) {
			LOG.debug("Fetching from CurrentElectionQueue:\n");
			Notification currentN = currentElectionQueue.poll();

			if (currentN == null) {
				LOG.debug("Notification Queue is empty!!");
				try {
					Thread.sleep(timeout);
					currentN = currentElectionQueue.poll();

					if (currentN == null) {
						LOG.debug("Notification Queue is empty again!!");
						timeout = 2 * timeout;
						LOG.debug("increasing timeout");

						// TODO: verify
						sendNotificationToAll(myNotification.toString());

					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			// CurrentN is not null

			else if (currentN.getSenderState() == NodeServerProperties1.State.ELECTION) {
				LOG.debug("Received notification is in Election");
				if (currentN.getSenderRound() < this.properties.getElectionRound()) {
					LOG.debug("Disregard vote as round number is smaller than mine");
					continue;
				} else {
					if (currentN.getSenderRound() > this.properties.getElectionRound()) {
						LOG.debug("The round number is larger than mine");
						this.properties.setElectionRound(currentN.getSenderRound());

						receivedVote = new HashMap<Long, Vote>();
						receivedVotesRound = new HashMap<Long, Long>();
					}
					LOG.debug("-------------------------");
					LOG.debug("myvote:" + this.properties.getMyVote());
					LOG.debug("othervote:" + currentN.getVote());
					LOG.debug("vote compare:" + currentN.getVote().compareTo(this.properties.getMyVote()));
					LOG.debug("-------------------------");
					if (currentN.getVote().compareTo(this.properties.getMyVote()) > 0) { // if
						// the
						// currentN
						// is
						// bigger
						// thn
						// myvote
						LOG.debug("His vote bigger than mine");
						this.properties.setMyVote(currentN.getVote()); // update
						// myvote
						myNotification.setVote(this.properties.getMyVote()); // update
						// notification

					}
					// TODO: verify
					sendNotificationToAll(myNotification.toString());

					// update the receivedVote datastructure
					LOG.debug("*****Vote for NodeID:" + currentN.getSenderId() + ":::" + currentN.getVote());
					receivedVote.put(currentN.getSenderId(), currentN.getVote());
					receivedVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
					// TODO should I put my vote in the receivedVote
					receivedVote.put(this.properties.getNodeId(), this.properties.getMyVote());
					receivedVotesRound.put(this.properties.getNodeId(), this.properties.getElectionRound());
					LOG.debug("*****receivedVote.size:" + receivedVote.size());
					LOG.debug("*****memberList.size:" + memberList.size());

					if (receivedVote.size() == (memberList.size() + 1)) {
						// TODO check for Quorum in the receivedvotes and then
						// declare leader
						LOG.info("Received Votes from all the members");
						for (Entry<Long, Vote> entries : receivedVote.entrySet()) {
							LOG.debug(entries.getKey() + "::" + entries.getValue());
						}

						break;
					} else {
						LOG.debug("*Checking for Quorum in received votes");
						int myVoteCounter = 0;
						for (Entry<Long, Vote> v : receivedVote.entrySet()) {
							Vote currVote = v.getValue();
							if (currVote.equals(this.properties.getMyVote())) {
								myVoteCounter++;
							}
						}
						if (myVoteCounter > (memberList.size() + 1) / 2) {
							LOG.info("Found Quorum in received votes");
							try {

								Thread.sleep(timeout);

								// Thread.sleep(timeout);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (currentElectionQueue.size() > 0) {
								LOG.debug("Still have notifications in ElectionQueue");
								continue;
							} else {
								LOG.info("And No more notifications in ElectionQueue");
								break;
							}
						} else {
							continue;
						}
					}

				}

			} // end of if election

			else { // the received vote is either leading or following
				if (currentN.getSenderRound() == this.properties.getElectionRound()) {
					LOG.debug(
							"Notification is not in election, round numbers of current node and current notification are same");
					receivedVote.put(currentN.getSenderId(), currentN.getVote());
					receivedVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());
					// TODO should i put my vote in the receivedVote
					// receivedVote.put(this.properties.getId(), myVote);
					// receivedVotesRound.put(this.properties.getId(),
					// this.electionRound);

					if (currentN.getSenderState() == NodeServerProperties1.State.LEADING) { // This
						// is
						// notification
						// from
						// leader
						LOG.debug("This notification is from the Leader");
						this.properties.setMyVote(currentN.getVote());
						break;
					} else {
						LOG.debug("This notification is from a Follower");
						int myVoteCounter = 0;
						for (Entry<Long, Vote> v : receivedVote.entrySet()) {
							Vote currVote = v.getValue();
							if (currVote.equals(this.properties.getMyVote())) {
								myVoteCounter++;
							}
						}
						// if the currentN's vote is to me and i achieve quorum
						// in receivedVote then i be the leader

						if (currentN.getVote().getId() == this.properties.getMyVote().getId()
								&& myVoteCounter > (memberList.size() + 1) / 2) {
							this.properties.setMyVote(currentN.getVote());
							break;
						} else if (myVoteCounter > (memberList.size() + 1) / 2) { // our
							// improvement
							this.properties.setMyVote(currentN.getVote());
							break;
						}
						// wrong condition
						// else if(myVoteCounter> (memberList.size()+1)/2
						// &&
						// OutOfElectionVotes.containsKey(currentN.getVote().getId())){
						// //TODO this is not 100% sure
						// myVote = currentN.getVote();
						// break;
						// }

					}

				}

				OutOfElectionVotes.put(currentN.getSenderId(), currentN.getVote());
				OutOfElectionVotesRound.put(currentN.getSenderId(), currentN.getSenderRound());

				int myVoteCounter = 0;
				for (Entry<Long, Vote> v : OutOfElectionVotes.entrySet()) {
					Vote currVote = v.getValue();
					if (currVote.equals(this.properties.getMyVote())) {
						myVoteCounter++;
					}
				}

				if (currentN.getVote().getId() == this.properties.getMyVote().getId()
						&& myVoteCounter > (memberList.size() + 1) / 2) {
					this.properties.setElectionRound(currentN.getSenderRound());
					this.properties.setMyVote(currentN.getVote());
					break;
				} else if (myVoteCounter > (memberList.size() + 1) / 2) { // our
					// improvement
					// just
					// check
					// for
					// the
					// quorum

					this.properties.setMyVote(currentN.getVote());
					break;

				}
				// //wrong condition
				// else if(myVoteCounter> (memberList.size()+1)/2 &&
				// OutOfElectionVotes.containsKey(currentN.getVote().getId())){
				// this.electionRound = currentN.getSenderRound();
				//
				// }

			} // end of else

		} // end of while
		// // Here the leader is the one pointed by my vote
		//
		if (this.properties.getMyVote().getId() == this.properties.getNodeId()) {
			this.properties.setLeader(true);
			this.properties.setLeaderId(this.properties.getNodeId());
			this.properties.setNodestate(NodeServerProperties1.State.LEADING);
		} else {
			this.properties.setLeader(false);
			this.properties.setLeaderId(this.properties.getMyVote().getId());
			this.properties.setNodestate(NodeServerProperties1.State.FOLLOWING);
		}

		this.properties.setAcceptedEpoch(this.properties.getLastZxId().getEpoch());

		ConcurrentHashMap<Long, Long> acceptedEpochMap = this.properties.getSynData().getAcceptedEpochMap();
		ConcurrentHashMap<Long, ZxId> currentEpochMap = this.properties.getSynData().getCurrentEpochMap();

		acceptedEpochMap.clear();
		currentEpochMap.clear();

		AtomicInteger quorumCount = new AtomicInteger(0);
		this.properties.getSynData().setQuorumCount(quorumCount);
		this.properties.getSynData().setNewEpochFlag(false);

		this.properties.getElectionQueue().clear();

		LOG.info("End of Leader Election Phase");
		long leEndTime = System.nanoTime();
		LOG.info("Time required for Leader Election Phase:" + (leEndTime-leStartTime)/1000000);
		LOG.info("Leader ID: " + this.properties.getMyVote().getId());

		return this.properties.getMyVote();

	}

	private void Recovery() {
		LOG.info("Start Recovery Phase");
		properties.reStartTime = System.nanoTime();
		
		long leaderID = properties.getLeaderId();

		if (this.properties.isLeader() == true) {
			// Leader
			LOG.debug("I am Leader");

			ConcurrentHashMap<Long, Long> acceptedEpochMap = this.properties.getSynData().getAcceptedEpochMap();

			// this.properties.getSynData().setAcceptedEpochMap(acceptedEpochMap);
			// this.properties.getSynData().setCurrentEpochMap(currentEpochMap);
			//
			LOG.debug("Member List size before while is = " + properties.getMemberList().size());
			LOG.debug("Accepted Epoch Map Size before while is = " + acceptedEpochMap.size());

			while (acceptedEpochMap.size() < this.properties.getMemberList().size() / 2) {
				// TODO: Figure out how to update memberlist size
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (properties.getNodestate() == NodeServerProperties1.State.ELECTION) {
					return;
				}
			}

			LOG.debug("Member List size after while is = " + properties.getMemberList().size());
			LOG.debug("Accepted Epoch Map Size after while is = " + acceptedEpochMap.size());

			long max = this.properties.getAcceptedEpoch();
			LOG.info("Accepted Epoch before max = " + max);

			for (long nodeId : acceptedEpochMap.keySet()) {
				long accEpoch = acceptedEpochMap.get(nodeId);
				if (accEpoch > max) {
					max = accEpoch;
				}
			}
			max += 1;
			this.properties.setNewEpoch(max);

			LOG.info("NewEpoch = " + max);
			this.properties.setAcceptedEpoch(max);
			this.properties.setCounter(0);

			synchronized (acceptedEpochMap) {
				this.properties.getSynData().setNewEpochFlag(true);
				acceptedEpochMap.notifyAll();
			}

			// String leaderLastLog = FileOps.readLastLog(properties);
			// String[] arr = leaderLastLog.split(",");
			// long epoch = Long.parseLong(arr[0].trim());
			// long counter = Long.parseLong(arr[1].trim());
			// ZxId leaderLastCommittedZxid = new ZxId(epoch, counter);
			//
			// ConcurrentHashMap<Long, ZxId> currentEpochMap =
			// this.properties.getSynData().getCurrentEpochMap();
			//
			// while(currentEpochMap.size() <=
			// this.properties.getMemberList().size()/2 ){
			// //TODO: Figure out how to update memberlist size
			// try {
			// Thread.sleep(10);
			// } catch (InterruptedException e){
			// e.printStackTrace();
			// }
			// }
			//
			// for (long nodeId : currentEpochMap.keySet()){

			// Map<Long, InetSocketAddress> memberList =
			// this.properties.getMemberList();

			// ZxId followerLastCommittedZxid = currentEpochMap.get(nodeId);
			//
			// if (leaderLastCommittedZxid.getEpoch() ==
			// followerLastCommittedZxid.getEpoch()){
			//
			// if (followerLastCommittedZxid.getCounter() <
			// leaderLastCommittedZxid.getCounter()){
			//
			// // TODO: Send DIFF message
			//
			// this.nettyClient.sendMessage(memberList.get(nodeId).getHostName(),
			// memberList.get(nodeId).getPort(), "DIFF");
			//
			// // TODO: Iterate through CommitHistory (refer readHistory()),
			// stringify and send
			//
			// } else if (followerLastCommittedZxid.getCounter() ==
			// leaderLastCommittedZxid.getCounter()){
			// continue;
			// } else if (followerLastCommittedZxid.getCounter() >
			// leaderLastCommittedZxid.getCounter()){
			// // Go to Leader Election. Ideally, shouldn't happen
			// this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
			// changePhase();
			// }
			//
			// } else if (followerLastCommittedZxid.getEpoch() <
			// leaderLastCommittedZxid.getEpoch()){
			//
			// // TODO: Send SNAP message

			// this.nettyClient.sendMessage(memberList.get(nodeId).getHostName(),
			// memberList.get(nodeId).getPort(), "SNAP");

			// // Iterate through the Map, stringify each entry and then send
			//
			// } else if (followerLastCommittedZxid.getEpoch() >
			// leaderLastCommittedZxid.getEpoch()){
			// // Go to Leader Election. Ideally, shouldn't happen
			// this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
			// changePhase();
			// }
			// }

			AtomicInteger quorumCount = this.properties.getSynData().getQuorumCount();

			while (quorumCount.intValue() < (this.properties.getMemberList().size() / 2)) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			long reEndTime = System.nanoTime();
			LOG.info("Time required for Recovery Phase:" + (reEndTime-properties.reStartTime)/1000000);
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			
			
			LOG.info("End of Recovery phase");
		} else {
			// Follower
			LOG.debug("I am Follower");
			InetSocketAddress leaderAddr = properties.getMemberList().get(leaderID);
			String leaderIp = leaderAddr.getHostName();
			int leaderPort = leaderAddr.getPort();
			// ZxId followerLastCommittedZxid = readHistory();
			// long currentEpoch = followerLastCommittedZxid.getEpoch();
			// this.properties.setCurrentEpoch(currentEpoch);

			String followerinfomsg = "FOLLOWERINFO:" + this.properties.getNodeId() + ":"
					+ this.properties.getAcceptedEpoch();

			LOG.info("FOLLOWERINFO : " + followerinfomsg);

			this.nettyClient.sendMessage(leaderIp, leaderPort, followerinfomsg);

			// Receive SNAP, DIFF and TRUNC messages

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			LOG.info("End of Recovery Phase");

		}

	}
	// TODO: Figure out how follower received newEpoch

	// while (!properties.getSynData().isNewEpochReceived()){
	// try {
	// Thread.sleep(10);
	// } catch (InterruptedException e){
	// e.printStackTrace();
	// }
	// }

	// this.properties.getSynData().setNewEpoch(0L);
	// LOG.debug("New Epoch before while is = " +
	// properties.getSynData().getNewEpoch());
	// while (this.properties.getSynData().getNewEpoch() == 0){
	// try {
	// Thread.sleep(10);
	// } catch (InterruptedException e){
	// e.printStackTrace();
	// }
	// }
	// LOG.debug("New Epoch before while is = " +
	// properties.getSynData().getNewEpoch());

	// long newEpoch = this.properties.getSynData().getNewEpoch();
	// long acceptedEpoch = this.properties.getAcceptedEpoch();
	////
	////
	// if (newEpoch > acceptedEpoch){
	// this.properties.setAcceptedEpoch(newEpoch);
	// this.properties.setCounter(0);
	//
	// String myLastLog = FileOps.readLastLog(properties);
	// String[] arr = myLastLog.split(",");
	// long currentEpoch = Long.parseLong(arr[0].trim());
	// long currentCounter = Long.parseLong(arr[1].trim());
	//
	// String ackepochmsg = "ACKNEWEPOCH:" + this.properties.getNodeId()
	// + ":" + currentEpoch + ":" + currentCounter; // TODO: currentepoch,
	// history, lastZxid
	//
	// this.nettyClient.sendMessage(leaderIp, leaderPort, ackepochmsg);
	//
	// } else {
	// this.properties.setNodestate(NodeServerProperties1.State.ELECTION);
	// changePhase();
	// }

	private void startBroadcast() {
		LOG.info("Start Broadcast Phase");
		
		long bcStartTime = System.nanoTime();
		LOG.info("Time required to reach Broadcast Phase:" + (properties.leStartTime-bcStartTime)/1000000);
		
		if (properties.getNodestate() == NodeServerProperties1.State.ELECTION) {
			changePhase();
		}
		// Clear proposal queue
		this.properties.getSynData().getProposedTransactions().clear();
		this.properties.getSynData().getCommittedTransactions().clear();
		MonitorProposeQueue monitorObj = null;
		WriteToDisk wtdObj = null;

		if (properties.getNodestate() == NodeServerProperties1.State.LEADING) {
			LOG.debug("Starting Monitor Propose Queue thread for leader...");
			monitorObj = new MonitorProposeQueue(properties, this);
			Thread threadMonitorProposeQueue = new Thread(monitorObj);
			threadMonitorProposeQueue.setPriority(Thread.MIN_PRIORITY);
			threadMonitorProposeQueue.start();
		}

		LOG.debug("Starting Write to disk thread irrespective of leader or follower...");
		if (properties.getNodestate() != NodeServerProperties1.State.ELECTION) {
			wtdObj = new WriteToDisk(properties);
			Thread threadWriteToDisk = new Thread(wtdObj);
			threadWriteToDisk.setPriority(Thread.MIN_PRIORITY);
			threadWriteToDisk.start();
		}

		while (properties.getNodestate() != NodeServerProperties1.State.ELECTION) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (monitorObj != null)
			monitorObj.stop();
		wtdObj.stop();

		LOG.info("End of Broadcast Phase");
		changePhase();

	}

	public void init() {

		LOG.info("Starting Node server");

		this.nettyClient = new NettyClient1(properties);

		msgBootstrap();

		for (Entry<Long, InetSocketAddress> entry : properties.getMemberList().entrySet()) {
			LOG.debug(entry.getKey() + ":::" + entry.getValue().getHostName() + ":" + entry.getValue().getPort());
		}

		LOG.info("Received Node ID: " + properties.getNodeId());

		this.nettyServerThread = new Thread(new NettyServer1(properties.getNodePort(), properties));
		this.nettyServerThread.setPriority(Thread.MIN_PRIORITY);
		this.nettyServerThread.start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		joinGroup();

		FileOps.fillDataInProperties(properties);

		changePhase();

	}

	// TODO: Commented for testing broadcast, uncomment later!!

	// Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler()
	// {
	// public void uncaughtException(Thread th, Throwable ex) {
	// LOG.debug(th.getName());
	// LOG.debug(th.getId());
	// LOG.debug(ex);
	// changePhase();
	// System.out.println("Uncaught exception: " + ex);
	//
	// }
	// };
	//
	//
	// private void changePhase() {
	// /*
	// The logic of changing phases
	// */
	// long leaderID = properties.getNodeId();
	// if(
	// properties.getNodestate().equals(NodeServerProperties1.State.ELECTION)){
	// LOG.debug("Begin Leader Election---------");
	// Vote leaderVote = startLeaderElection();
	// LOG.debug("End Leader Election---------");
	// LOG.debug("Leader ID:"+leaderVote.getId() );
	// if(leaderVote.getId() == properties.getNodeId()){
	// properties.setLeader(true);
	// properties.setNodestate(NodeServerProperties1.State.LEADING);
	// leaderID = properties.getNodeId();
	//
	// this.udpServerThread = new Thread(new UdpServer1(properties));
	// this.udpServerThread.setPriority(Thread.MIN_PRIORITY);
	// this.udpServerThread.setUncaughtExceptionHandler(h);
	// this.udpServerThread.start();
	//
	// }
	// else{
	// properties.setLeader(false);
	// properties.setNodestate(NodeServerProperties1.State.FOLLOWING);
	// leaderID = leaderVote.getId();
	// properties.setLeaderId(leaderID);
	// InetSocketAddress leaderis = properties.getMemberList().get(leaderID);
	// properties.setLeaderAddress(leaderis);
	//
	// this.udpClientThread = new Thread(new UdpClient1(properties));
	// this.udpClientThread.setPriority(Thread.MIN_PRIORITY);
	// this.udpClientThread.setUncaughtExceptionHandler(h);
	// this.udpClientThread.start();
	//
	//
	//
	// }
	// }
	//
	//
	// while(true){
	//
	// if(properties.getNodestate() != NodeServerProperties1.State.ELECTION ){
	// try {
	// Thread.sleep(4000);
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// else{
	// changePhase();
	// }
	// }
	//
	// //startRecovery();
	// //startBroadcast();
	// }

	public void mockData() {
		LOG.debug("Mocking the data for testing broadcast...!!");
		InetSocketAddress leaderAddress = new InetSocketAddress("localhost", 9001);
		this.properties.setLeaderAddress(leaderAddress);
		this.properties.setAcceptedEpoch(3);
		this.properties.setCounter(1);

		if (this.properties.getNodePort() == 9001) {
			this.properties.setLeader(true);
			// this.properties.setNodeId(1L);

			this.properties.setNodestate(NodeServerProperties1.State.LEADING);
		} else {
			this.properties.setLeader(false);
			// this.properties.setNodeId(2L);
			this.properties.setNodestate(NodeServerProperties1.State.FOLLOWING);
		}
	}

	Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread th, Throwable ex) {
			LOG.debug(th.getName());
			LOG.debug(th.getId());
			LOG.debug(ex.getMessage());
			
			LOG.info("Leader is Unreachable!");
			LOG.debug("Uncaught exception: " + ex);

		}
	};

	private void changePhase() {
		/*
		 * The logic of changing phases
		 */
		long leaderID = properties.getNodeId();
		if (properties.getNodestate().equals(NodeServerProperties1.State.ELECTION)) {

			Vote leaderVote = startLeaderElection();

			if (leaderVote.getId() != properties.getNodeId()) {
				LOG.debug("Sleeping for 3 seconds");
				LOG.debug("For synchronizing with the Leader");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

			if (leaderVote.getId() == properties.getNodeId()) {
				properties.setLeader(true);
				properties.setNodestate(NodeServerProperties1.State.LEADING);
				leaderID = properties.getNodeId();

				this.udpServerThread = new Thread(new UdpServer1(properties));
				this.udpServerThread.setPriority(Thread.MIN_PRIORITY);
				this.udpServerThread.setUncaughtExceptionHandler(h);
				this.udpServerThread.start();

			} else {
				properties.setLeader(false);
				properties.setNodestate(NodeServerProperties1.State.FOLLOWING);
				leaderID = leaderVote.getId();
				properties.setLeaderId(leaderID);
				InetSocketAddress leaderis = properties.getMemberList().get(leaderID);
				properties.setLeaderAddress(leaderis);

				this.udpClientThread = new Thread(new UdpClient1(properties));
				this.udpClientThread.setPriority(Thread.MIN_PRIORITY);
				this.udpClientThread.setUncaughtExceptionHandler(h);
				this.udpClientThread.start();
			}

			properties.getSynData().getElectionQueue().clear();

			Recovery();

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			startBroadcast();
		}

		// while(true){
		//
		// if(properties.getNodestate() != NodeServerProperties1.State.ELECTION
		// ){
		// try {
		// Thread.sleep(4000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// else{
		// changePhase();
		// }
		// }

	}

	public long msgBootstrap() {
		Socket socket;
		long id = 0;
		try {
			socket = new Socket(properties.getBootstrapHost(), properties.getBootstrapPort());

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// set self_ip:port to bootstrap
			LOG.debug("set " + properties.getBootstrapHost() + ":" + properties.getBootstrapPort());
			LOG.debug("set " + properties.getNodeHost() + ":" + properties.getNodePort());

			out.println("set " + properties.getNodeHost() + ":" + properties.getNodePort());

			String memberList = in.readLine();
			String memberId = in.readLine();
			id = Long.parseLong(memberId);
			LOG.debug("MemberID received:" + id);
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

		LOG.info("Received MembersList :\n" + memberList);

		String[] list = memberList.split(",");

		for (String s : list) {
			String[] address = s.split(":");
			long nodeId = Integer.parseInt(address[0]);
			String ip = address[1];
			int port = Integer.parseInt(address[2]);
			if (properties.getNodeHost().equals(ip) && properties.getNodePort() == port) {

			} else {
				InetSocketAddress addr = new InetSocketAddress(address[1], Integer.parseInt(address[2]));
				LOG.info("Adding" + nodeId + ":::" + addr);
				properties.addMemberToList(nodeId, addr);

			}

		}
		LOG.debug("-----------MemberListSize:" + properties.getMemberList().size());

	}

	private void sendNotificationToAll(String message) {
		LOG.debug("SendNotificationToAll()" + message);
		broadcast("CNOTIFICATION:" + message);
	}

	private void joinGroup() {
		LOG.info("Send JOIN_GROUP message");
		broadcast("JOIN_GROUP:" + properties.getNodeId() + ":" + properties.getNodeHost() + ":"
				+ properties.getNodePort());
	}

	public void broadcast(String message) {

		// LOG.debug("*******MemberlistSize:"+properties.getMemberList().size());
		LOG.debug("Inside Broadcast::Message:" + message);
		Map<Long, InetSocketAddress> unreachablelist = new HashMap<Long, InetSocketAddress>();
		for (Entry<Long, InetSocketAddress> member : properties.getMemberList().entrySet()) {
			try {
				LOG.debug("Sending " + message + " to: " + member.getValue().getHostName() + ":"
						+ member.getValue().getPort());
				this.nettyClient.sendMessage(member.getValue().getHostName(), member.getValue().getPort(), message);
			} catch (Exception e) {
				unreachablelist.put(member.getKey(), member.getValue());
				LOG.error(e.getMessage());
				e.printStackTrace();
			}
		}

		for (Entry<Long, InetSocketAddress> member : unreachablelist.entrySet()) {
			LOG.debug("Removing from memberlist" + member.getKey() + ":" + member.getValue());
			properties.removeMemberFromList(member.getKey());
		}
	}

	// private ZxId readHistory() {
	//
	// String fileName = "CommitedHistory_" + properties.getNodePort() + ".txt";
	// String line = null;
	// Queue<Message> msgList = properties.getCommitQueue();
	// try {
	//
	// FileReader fileReader = new FileReader(fileName);
	// BufferedReader bufferedReader = new BufferedReader(fileReader);
	//
	// while ((line = bufferedReader.readLine()) != null) {
	// Message m = new Message(line);
	// msgList.add(m);
	// System.out.println(m);
	// }
	//
	// bufferedReader.close();
	// fileReader.close();
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// Message[] msgArr = new Message[msgList.size()];
	// msgList.toArray(msgArr);
	//
	// Message lastMsg = msgArr[msgArr.length - 1];
	//
	// this.properties.setCurrentEpoch(lastMsg.getZxid().getEpoch());
	// this.properties.setLastZxId(lastMsg.getZxid());

	// LOG.debug("ZxId of last message is = Epoch: " +
	// lastMsg.getZxid().getEpoch() + " Counter: " +
	// lastMsg.getZxid().getCounter());

	//
	// return this.properties.getLastZxId();
	//
	// }

}
