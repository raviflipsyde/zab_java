package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TcpClient implements Runnable {
	
	private static final Logger LOG = LogManager.getLogger(TcpClient.class);
	private String host;
	private int port;
	private String myip;
	

	public TcpClient(String host, int port, String myIp) {
		this.host  = host;
		this.port = port;
		this.myip = myIp;
	}

	public void run() {
		
		Socket socket = null;
		BufferedReader in;
		PrintWriter out;
		try {
		LOG.debug("Started a TCP Client ");
		
		
			LOG.debug("Socket for "+this.host+":"+this.port);
			socket = new Socket( host, port );
			
			
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			
			LOG.debug("Sending node info to groupmember");
			out.println("JOIN_GROUP:"+socket.getInetAddress()+":"+socket.getPort()+"\r\n");
			out.flush();
			
			String request = in.readLine();
			StringBuilder strb = new StringBuilder();
			while(request!=null && request.length() > 0){
				strb.append(request);
				request = in.readLine();
			}
			
			LOG.debug("Recieved from server: "+ strb.toString());
			
			in.close();
			out.close();
			socket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.debug(e.getMessage());
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.debug(e.getMessage());
		}
		
		finally {
			// TODO Auto-generated finally block
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOG.debug(e.getMessage());
			}
			
		}

	}

	private String processRequest(String input) {
		return "Wow!! Received "+input;
		
	}
	
	

}
