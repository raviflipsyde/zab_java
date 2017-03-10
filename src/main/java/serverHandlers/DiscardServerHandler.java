package serverHandlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Handles a server-side channel.
 */
public class DiscardServerHandler extends ChannelInboundHandlerAdapter { // (1)
	private static final Logger LOG = LogManager.getLogger(DiscardServerHandler.class);
	private final static String fileName = "members.txt";
	private final String defaultPort = "8080";
	static{
		
		File file = new File(fileName);
		try {
			// if file doesnt exists, then create it
			if (file.exists()) {
				
				file.delete();
			}

			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}
	public DiscardServerHandler() {
		super();

		

	}



	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		ByteBuf in = (ByteBuf) msg;
		String requestMsg  =in.toString(StandardCharsets.UTF_8 );
		LOG.info("Server Recieved : "+requestMsg);
		String memberList = handleClientRequest(requestMsg);
		LOG.info("Member List : "+memberList);

		ctx.write(Unpooled.copiedBuffer(memberList+"\r\n", StandardCharsets.UTF_8));

		ctx.flush(); // (2)

	}



	private String handleClientRequest(String requestMsg) {
		String ret = "";
		if(requestMsg.contains("get")){

			// TODO Read the config file and send the data to client;
			ret = getMembersList();
		}
		else if(requestMsg.contains("set")){
			// TODO read the client request to fetch parameters and save in the config files.
			if(setMembersList(requestMsg).equals("ok")){
				ret = getMembersList();
			}
			// TODO inform the rest of the servers in config about the new member in the group

		}

		return ret;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}


	public String getMembersList(){
		StringBuilder memberList= new StringBuilder();
		String line = null;
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while((line = bufferedReader.readLine()) != null) {
				memberList.append(line);
				memberList.append(",");
			} 

			bufferedReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return memberList.toString();
	}

	public String setMembersList(String member){

		try {
			FileWriter fileWriter = new FileWriter(fileName,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			member = formatMembersName(member);
			//bufferedWriter.newLine();
			bufferedWriter.write(member+"\n");
			bufferedWriter.flush();
			bufferedWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "error";
		}

		return "ok";
	}

	public void informGroupMembers(){

	}

	public String formatMembersName(String member){
		String[] array = member.split(" ");

		//TODO check the port range

		if(array[1].indexOf(":")>-1){
			if(array[1].indexOf(":")!=array[1].trim().length()-1)
				return array[1].trim();
			else
				return array[1].trim()+defaultPort;
		}
		else{
			return array[1].trim()+":"+defaultPort;
		}
	}


}