package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpClient implements Runnable {
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
			socket = new Socket( host, port );
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			
			out.println("Hi.."+"\r\n");
			out.flush();
			
			String request = in.readLine();
			StringBuilder strb = new StringBuilder();
			while(request!=null && request.length() > 0){
				strb.append(request);
				request = in.readLine();
			}
				
			System.out.println("Recieved from server:"+ strb.toString());
			
			in.close();
			out.close();
			socket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		finally {
			// TODO Auto-generated finally block
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private String processRequest(String input) {
		return "Wow!! Received "+input;
		
	}

}
