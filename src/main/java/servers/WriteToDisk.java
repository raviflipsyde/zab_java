package servers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
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
		LOG.debug("Run method for WriteToDisk thread");
		LOG.debug("Node is in " + properties.getNodestate().toString() + " state");
		while( properties.getNodestate() != NodeServerProperties1.State.ELECTION && running == true){
			
			//flush the committed transactions set
			SortedSet<Proposal> committedtransactions = properties.getSynData().getCommittedTransactions();
			synchronized (committedtransactions) {
				for(Proposal entry: committedtransactions){
					LOG.debug("WriteToDisk: Writing transactions to Transaction log:");
					String entry_commit_history = entry.toString();
					LOG.debug("entry_commit_history:"+ entry_commit_history);
					
					String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
					FileOps.appendTransaction(fileName,entry_commit_history);
					
					//TODO: Write to datamap
					String[] arr = entry_commit_history.split(":");
					
					
					String key = arr[2].trim();
					String value = arr[3].trim();
					
					
					Properties datamap = properties.getDataMap();
					datamap.setProperty(key, value);
					
				}
				
				FileOps.writeDataMap(properties);
				
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
	
	public void stop(){
		running = false;
	}

}
