package servers;

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonitorProposeQueue implements Runnable {
	private NodeServerProperties1 nodeserverproperties;
	private NodeServer1 nodeserver;
	
	private static final Logger LOG = LogManager.getLogger(MonitorProposeQueue.class);
	
	public MonitorProposeQueue(NodeServerProperties1 nodeServerProperties1, NodeServer1 nodeServer) {
		this.nodeserverproperties = nodeServerProperties1;
		this.nodeserver =  nodeServer;

	}

	public void run() {
			
		LOG.info("Run method for MonitorProposeQueue");
		
		ConcurrentHashMap<Proposal, Long> removeMap = new ConcurrentHashMap<Proposal, Long>();
		ConcurrentHashMap<Proposal, Long> proposedtransactions = this.nodeserverproperties.getSynData().getProposedTransactions();

		while(nodeserverproperties.getNodestate() == NodeServerProperties1.State.LEADING){
			
			for( Entry<Proposal,Long> entry: proposedtransactions.entrySet()){
				//if(entry.getValue() >= this.nodeserverproperties.getMemberList().size()/2){
					LOG.info("Quorum achieved for Proposal:" + entry.getKey());
					LOG.info("Sending a COMMIT message now to all followers..!!");
					String commitMessage = "COMMIT:"+ entry.getKey();
					nodeserver.broadcast(commitMessage);
					
					//Adding to commit Queue
					SortedSet<Proposal> committedtransactions = nodeserverproperties.getSynData().getCommittedTransactions();
					committedtransactions.add(entry.getKey());
					
					
					//Adding the entry to remove Queue
					removeMap.put(entry.getKey(), entry.getValue());
				//}
			}	
		
			LOG.info( removeMap.size() + "transactions to be removed");
			
			for (Entry<Proposal, Long> entry : removeMap.entrySet()) {
				LOG.info("Removing from Proposed transactions Map"+ entry.getKey()+":"+ entry.getValue());
				proposedtransactions.remove(entry.getKey());
			}
			
			try {
				//TODO: Decide how long to wait
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			
			}
		
		}
	}
		

}
