package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TcpRequestHandler implements Runnable {
	
	private static final Logger LOG = LogManager.getLogger(TcpRequestHandler.class);
	private Socket socket;

	public TcpRequestHandler(Socket socket) {
		this.socket = socket;

	}

	public void run() {
		BufferedReader in;
		PrintWriter out;
		try {
			
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

			String request = in.readLine();
			
			LOG.debug("Received: "+ request);
			
			String output = processRequest(request);
			
			LOG.debug("Sending response:"+ output);
			
			out.println(output+"\r\n");
			out.flush();
			
			in.close();
            out.close();
            socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		finally {
			// TODO Auto-generated finally block
		}

	}

	private String processRequest(String input) {
		return "Hi back..";
		
	}

}
