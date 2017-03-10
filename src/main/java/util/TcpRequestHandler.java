package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpRequestHandler implements Runnable {

	private Socket socket;

	public TcpRequestHandler(Socket socket) {
		this.socket = socket;

	}

	public void run() {
		BufferedReader in;
		PrintWriter out;
		try {
			System.out.println("A Request Received");
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			String request = in.readLine();
			StringBuilder strb = new StringBuilder();
			while(request!=null && request.length() > 0){
				strb.append(request);
				request = in.readLine();
			}
			
			String output = processRequest(strb.toString());
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
		return "Wow!! Received "+input;
		
	}

}
