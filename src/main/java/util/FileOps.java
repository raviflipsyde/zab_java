package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import servers.NodeServerProperties1;
import servers.ZxId;

public class FileOps {

	// write a function to write the line into the CommitedHistory file
	private static final Logger LOG = LogManager.getLogger(FileOps.class);
	private static FileWriter appendLogFileWriter;
	private static BufferedWriter appendLogFileBufferWriter;
	
	public static String appendTransaction(NodeServerProperties1 properties,String transaction) {
		
		String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
		
		if(appendLogFileWriter != null){
			try {
				appendLogFileWriter = new FileWriter(fileName,true);
				appendLogFileBufferWriter = new BufferedWriter(appendLogFileWriter);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		try {
			transaction = transaction.replaceAll(":", ",");
			
			
			appendLogFileBufferWriter.newLine();
			appendLogFileBufferWriter.write(transaction);

			appendLogFileBufferWriter.flush();
			
			
			String[] arr = transaction.split(",");
			long epoch = Long.parseLong(arr[0].trim());
			long counter = Long.parseLong(arr[1].trim());
			String key = arr[2].trim();
			String value = arr[3].trim();
			
			
			properties.getDataMap().put(key, value);
			LOG.debug("Transaction received-"+ transaction);
			LOG.debug("Adding to PropertiesMap-"+ key + ":" + value);
			
			properties.setLastZxId(new ZxId(epoch, counter));
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "appendTransaction:Error";
		}
		
		return "appendTransaction:Success";
	}


	
public static void writeDataMap(NodeServerProperties1 properties){
		
		String fileName = "datamap_" + properties.getNodePort() + ".properties";

		Properties dataMap = properties.getDataMap();
		LOG.debug("Start writeDataMap to "+ fileName);
		LOG.debug("Properties: "+ dataMap);
		try {
			FileOutputStream out = new FileOutputStream(fileName);
			dataMap.store(out,null);
			
			out.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			LOG.debug("File not found "+ fileName);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOG.debug("Some IO exception on "+ fileName);
			e.printStackTrace();
		}
		LOG.debug("End writeDataMap method");

	}

	public static Properties readDataMap(NodeServerProperties1 properties) {

		String fileName = "datamap_" + properties.getNodePort() + ".properties";

		Properties dataMap = new Properties();
		dataMap.clear();
		try {

			FileReader fileReader = new FileReader(fileName);
			dataMap.load(fileReader);
			fileReader.close();
		} catch (FileNotFoundException e) {
			
			try {
				PrintWriter writer = new PrintWriter(fileName, "UTF-8");
				writer.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			
			
			return dataMap;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dataMap;

	}

	public static String readLastLine(File file) {
		String ret = null;

		try {
			ReversedLinesFileReader reveFileReader = new ReversedLinesFileReader(file, Charset.defaultCharset());
			ret = reveFileReader.readLine();
			reveFileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;

	}

	public static String readLastLog(NodeServerProperties1 properties) {
		String ret = null;
		String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
		File file = new File(fileName);
		long epoch;
		long counter;
		String key;
		String value;

		try {
			ReversedLinesFileReader reveFileReader = new ReversedLinesFileReader(file, Charset.defaultCharset());
			ret = reveFileReader.readLine();
			if(ret==null || ret.length()==0)
				ret = "0,0,0,0";
			reveFileReader.close();
		} catch (IOException e) {
			
			
			try {
				PrintWriter writer = new PrintWriter(fileName, "UTF-8");
				writer.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			return "0,0,0,0";
		}

		return ret;

	}

	public static List<String> getDiffResponse(NodeServerProperties1 properties, ZxId zxid) {

		String ret = null;
		String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
		File file = new File(fileName);
		long epoch = 0, counter = 0;
		// String key,value;
		List<String> logList = new ArrayList<String>();

		try {
			ReversedLinesFileReader reveFileReader = new ReversedLinesFileReader(file, Charset.defaultCharset());

			while (true) {
				ret = reveFileReader.readLine();
				if (ret == null || ret.length() == 0)
					break;
				String[] arr = ret.split(",");
				epoch = Long.parseLong(arr[0].trim());
				counter = Long.parseLong(arr[1].trim());
				if (epoch == zxid.getEpoch() && counter == zxid.getCounter())
					break;
				logList.add(0, ret);
			}

			reveFileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return logList;

	}

	public static void fillDataInProperties(NodeServerProperties1 properties) {
		
		
		Properties datamap = readDataMap(properties);
		
		String lastLine = readLastLog(properties);
		

		String[] arr = lastLine.split(",");
		long epoch = Long.parseLong(arr[0].trim());
		long counter = Long.parseLong(arr[1].trim());

		properties.setLastEpoch(epoch);
		properties.setCounter(counter);
		ZxId id0 = new ZxId(epoch, counter);
		properties.setLastZxId(id0);
		properties.setDataMap(datamap);

	}

	public static void main(String[] args) {
		NodeServerProperties1 p1 = new NodeServerProperties1();
		p1.setNodePort(9001);
		// ZxId id1 = new ZxId(3, 0);
		// List<String> retList = getDiffResponse(p1,id1 );
		//
		// System.out.println(retList);
		fillDataInProperties(p1);
		
		

	}

}