package servers;

import java.util.Map.Entry;
import java.util.SortedMap;
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
			
		LOG.debug("Run method for MonitorProposeQueue");
		SortedMap<Proposal, AtomicInteger> proposedtransactions = this.nodeserverproperties.getSynData().getProposedTransactions();

		while(nodeserverproperties.getNodestate() == NodeServerProperties1.State.LEADING && running == true){
			ConcurrentHashMap<Proposal, AtomicInteger> removeMap = new ConcurrentHashMap<Proposal, AtomicInteger>();
			for( Entry<Proposal,AtomicInteger> entry: proposedtransactions.entrySet()){
				
				LOG.debug("----------Proposal:" + entry.getKey()+ "::" + entry.getValue());
				
				if(entry.getValue().get() > this.nodeserverproperties.getMemberList().size()/2
						&&
						entry.getKey().getZ().compareTo(nodeserverproperties.getLastZxId() ) > 0 
						){
					//if(this entry is not smaller than last entry f commitedHistory)
					
					LOG.debug("Quorum achieved for Proposal:" + entry.getKey());
					LOG.debug("Sending a COMMIT message now to all followers..!!");
					String commitMessage = "COMMIT:"+ entry.getKey();
					LOG.info("-----Leader sending ::"+ commitMessage);
					nodeserver.broadcast(commitMessage);
					
					//Adding to commit Queue
					SortedSet<Proposal> committedtransactions = nodeserverproperties.getSynData().getCommittedTransactions();
					committedtransactions.add(entry.getKey());
					
					//Putting in removemap and deleting later to avoid concurrent modification exception
					removeMap.put(entry.getKey(), entry.getValue());

					
				}
			}	
		
			//LOG.debug( removeMap.size() + "transactions to be removed");
			
			for (Entry<Proposal, AtomicInteger> entry : removeMap.entrySet()) {
				LOG.debug("Removing from Proposed transactions Map"+ entry.getKey()+":"+ entry.getValue());
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
	
	public void stop(){
		this.running = false;
	}
		

}
