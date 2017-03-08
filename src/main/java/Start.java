/**
 * Created by ravipatel on 3/2/17.
 */


import org.apache.logging.log4j.Logger;

import servers.BootstrapServer;
import servers.NodeServer;

import org.apache.logging.log4j.LogManager;

public class Start {

	// star	
	private static final Logger LOG = LogManager.getLogger(Start.class);

	public static void main(String[] args){

		//    	 LOG.debug("This will be printed on debug");
		//         LOG.info("This will be printed on info");
		//         LOG.warn("This will be printed on warn");
		//         LOG.error("This will be printed on error");
		//         LOG.fatal("This will be printed on fatal");
		if(args.length < 2){
			System.out.println("Too Few Parameters. \n Start client or Start Server port");
			System.exit(0);
		}



		if(args[0].equals("Client")){
			//    		new NodeServer("127.0.0.1", 4500).run1();
			String host;
			int port;
			System.out.println(args.length);
			if (args.length > 2) {
				port = Integer.parseInt(args[2]);
				host = args[1];
			} 
			else {
				host = "127.0.0.1";
				port = 8080;
			}
			LOG.info("Host:" + host);
			LOG.info("Port:" + port);

			new NodeServer(host, port).msgBootstrap();
		}

		else if(args[0].equals("Server")){

			int port;
			if (args.length > 0) {
				port = Integer.parseInt(args[1]);
			} else {
				port = 8080;
			}

			LOG.info("Port:" + port);

			try {
				new BootstrapServer(port).run();
			} catch (Exception e) {
				e.printStackTrace();
			}


		}

	}

	public static String printHello(){

		System.out.println("Hello World");
		return  "Hello World";
	}
}
