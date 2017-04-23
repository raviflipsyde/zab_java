package servers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.SortedSet;
import util.FileOps;

public class WriteToDisk implements Runnable {

	private NodeServerProperties1 properties;
	
	private static final Logger LOG = LogManager.getLogger(WriteToDisk.class);
	private volatile boolean running = true;
	
	public WriteToDisk(NodeServerProperties1 nodeProperties) {

		this.properties = nodeProperties;
			
	}
	public void run() {
		LOG.info("Run method for WriteToDisk thread");
		LOG.info("Node is in " + properties.getNodestate().toString() + " state");
		while( properties.getNodestate() != NodeServerProperties1.State.ELECTION && running == true){
			
			//flush the committed transactions set
			SortedSet<Proposal> committedtransactions = properties.getSynData().getCommittedTransactions();
			synchronized (committedtransactions) {
				for(Object entry: committedtransactions){
					LOG.info("WriteToDisk: Writing transactions to Transaction log:");
					String s = entry.toString();
					String fileName = "TestReplication_" + properties.getNodePort() + ".log";
					FileOps.appendTransaction(fileName,s);
					
					//TODO: Write to datamap
				}
				
				committedtransactions.clear();
				
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

}
