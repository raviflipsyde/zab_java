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

public class TcpClient1 {

	private static final Logger LOG = LogManager.getLogger(TcpClient1.class);
	private String host;
	private int port;

	public TcpClient1(String host, int port) {
		this.host = host;
		this.port = port;

	}

	public String sendMsg(String msg) {

		Socket socket = null;
		BufferedReader in;
		PrintWriter out;
		String retMsg = "";
		try {

			LOG.info("Started a TCP Client for " + this.host + ":" + this.port + " to send \n\"" + msg + "\"");

			socket = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			out.println(msg);
			// out.println("JOIN_GROUP:"+socket.getInetAddress()+":"+socket.getPort()+"\r\n");
			out.flush();

			String request = in.readLine();

//			StringBuilder strb = new StringBuilder();
//			while (request != null && request.length() > 0) {
//				strb.append(request);
//				request = in.readLine();
//			}
//			retMsg = strb.toString();
			
			retMsg = request;
			
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.info(e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.info(e.getMessage());
		}

		finally {
			// TODO Auto-generated finally block
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOG.info(e.getMessage());
			}

		}

		return retMsg;
	}

}
