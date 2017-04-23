package servers;

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonitorProposeQueue implements Runnable {
	private NodeServerProperties1 nodeserverproperties;
	private NodeServer1 nodeserver;
	
	private static final Logger LOG = LogManager.getLogger(MonitorProposeQueue.class);
	private volatile boolean running;
	
	public MonitorProposeQueue(NodeServerProperties1 nodeServerProperties1, NodeServer1 nodeServer) {
		this.nodeserverproperties = nodeServerProperties1;
		this.nodeserver =  nodeServer;
		this.running = true;

	}


	public void run() {
			
		LOG.info("Run method for MonitorProposeQueue");
		ConcurrentHashMap<Proposal, AtomicInteger> proposedtransactions = this.nodeserverproperties.getSynData().getProposedTransactions();

		while(nodeserverproperties.getNodestate() == NodeServerProperties1.State.LEADING && running == true){
			
			ConcurrentHashMap<Proposal, AtomicInteger> removeMap = new ConcurrentHashMap<Proposal, AtomicInteger>();
			
			//Iterating over proposedtransactions to check commit-ready entries
			for( Entry<Proposal,AtomicInteger> entry: proposedtransactions.entrySet()){
				
				if(entry.getValue().get() > this.nodeserverproperties.getMemberList().size()/2){
					LOG.info("Quorum achieved for Proposal:" + entry.getKey());
					LOG.info("Sending a COMMIT message now to all followers..!!");
					String commitMessage = "COMMIT:"+ entry.getKey();
					nodeserver.broadcast(commitMessage);
					
					//Adding to commit Queue
					SortedSet<Proposal> committedtransactions = nodeserverproperties.getSynData().getCommittedTransactions();
					committedtransactions.add(entry.getKey());
					
					//Putting in removemap and deleting later to avoid concurrent modification exception
					removeMap.put(entry.getKey(), entry.getValue());					
				}
			}	
		
			
			for (Entry<Proposal, AtomicInteger> entry : removeMap.entrySet()) {
				LOG.info("Removing from Proposed transactions Map"+ entry.getKey()+":"+ entry.getValue());
				proposedtransactions.remove(entry.getKey());
			}
			
			removeMap.clear();
			
			try {
				//TODO: Decide how long to wait
				Thread.sleep(500);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			
			}
		
		}
	}
		

}
