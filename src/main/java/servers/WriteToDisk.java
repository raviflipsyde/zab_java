package servers;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.FileOps;

//import netty.NettyClient1;
//import servers.NodeServerProperties1.State;

public class WriteToDisk implements Runnable {

	private NodeServerProperties1 properties;
	
	private static final Logger LOG = LogManager.getLogger(WriteToDisk.class);
	
	public WriteToDisk(NodeServerProperties1 nodeProperties) {

		this.properties = nodeProperties;
			
	}
	public void run() {
		//LOG.info("Run method for WriteToDisk thread");
		while( properties.getNodestate() != NodeServerProperties1.State.ELECTION){
			
			//flush the committed transactions set
			SortedSet<Proposal> committedtransactions = properties.getSynData().getCommittedTransactions();
			for(Object entry: committedtransactions){
				LOG.info("WriteToDisk: Writing transactions to Transaction log");
				String s = entry.toString();
				String fileName = "TestReplication_" + properties.getNodePort() + ".log";
				FileOps.appendTransaction(fileName,s);
				
				//TODO: Write to datamap
			}
			committedtransactions.clear();
		}
		
	}

}
