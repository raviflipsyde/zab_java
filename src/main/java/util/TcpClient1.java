package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author rpatel16
 *
 */
public class TcpClient1 {

	private static final Logger LOG = LogManager.getLogger(TcpClient1.class);
	private String host;
	private int port;

	public TcpClient1(String host, int port) {
		this.host = host;
		this.port = port;

	}

	public String sendMsg(String msg) throws IOException {

		Socket socket = null;
		BufferedReader in;
		PrintWriter out;
		String retMsg = "";
		try {

			LOG.debug("Started a TCP Client for " + this.host + ":" + this.port + " to send \n\"" + msg + "\"");

			socket = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			out.println(msg);
			out.flush();

			String request = in.readLine();

			retMsg = request;
			
			in.close();
			out.close();
			socket.close();
		} 
				finally {
			// TODO Auto-generated finally block
			try {
				if(socket!=null)
					socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOG.debug(e.getMessage());
			}

		}

		return retMsg;
	}

}
