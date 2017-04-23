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
		LOG.info("Run method for WriteToDisk thread");
		LOG.info("Node is in " + properties.getNodestate().toString() + " state");
		while( properties.getNodestate() != NodeServerProperties1.State.ELECTION && running == true){
			
			//flush the committed transactions set
			SortedSet<Proposal> committedtransactions = properties.getSynData().getCommittedTransactions();
			synchronized (committedtransactions) {
				for(Object entry: committedtransactions){
					LOG.info("WriteToDisk: Writing transactions to Transaction log:");
					String entry_commit_history = entry.toString();
					String fileName = "TestReplication_" + properties.getNodePort() + ".log";
					FileOps.appendTransaction(fileName,entry_commit_history);
					
					//Write to in-memory datamap
					String[] arr = entry_commit_history.split(":");
					//Long epoch = Long.parseLong(arr[1].trim());
					//Long counter = Long.parseLong(arr[2].trim());

					String key = arr[3].trim();
					String value = arr[4].trim();

					
					Properties datamap = properties.getDataMap();
					datamap.setProperty(key, value);
				}
				
				//Writing datamap to the properties file all at once
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

}
