package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.input.ReversedLinesFileReader;

import servers.NodeServerProperties1;
import servers.ZxId;

public class FileOps {

	
	
	public static Properties readDataMap(NodeServerProperties1 properties) {
		
		String fileName = "datamap_" + properties.getNodePort() + ".properties";
		
		Properties dataMap = new Properties();
		dataMap.clear();
		try {

			FileReader fileReader = new FileReader(fileName);
			dataMap.load(fileReader);
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return dataMap;

	}
	

	public static String readLastLine(File file ){
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
	
	public static String readLastLog(NodeServerProperties1 properties ){
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
			
			reveFileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
		
	}
	
	public static List<String> getDiffResponse(NodeServerProperties1 properties, ZxId zxid ){

		String ret = null;
		String fileName = "CommitedHistory_" + properties.getNodePort() + ".log";
		File file = new File(fileName);
		long epoch = 0,counter = 0;
//		String key,value;
		List<String> logList = new ArrayList<String>();
		
		try {
			ReversedLinesFileReader reveFileReader = new ReversedLinesFileReader(file, Charset.defaultCharset());
			
			while(true){
				ret = reveFileReader.readLine();
				if(ret==null || ret.length()==0) break;
				String[] arr = ret.split(",");
				epoch = Long.parseLong(arr[0].trim());
				counter = Long.parseLong(arr[1].trim());
				if(epoch == zxid.getEpoch() && counter == zxid.getCounter()) break;
				logList.add(0, ret);
			}
			
			reveFileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return logList;
		
	}
	
	public static void fillDataInProperties(NodeServerProperties1 properties){
		Properties datamap = readDataMap(properties);
		String lastLine = readLastLog(properties);
		System.out.println(lastLine);
		
		String[] arr = lastLine.split(",");
		long epoch = Long.parseLong(arr[0].trim());
		long counter = Long.parseLong(arr[1].trim());
		
		properties.setLastEpoch(epoch);
		properties.setCounter(counter);
		ZxId id0 = new ZxId(epoch, counter);
		properties.setLastZxId(id0);
		properties.setDataMap(datamap);
		
	}
	
	
	
	public static void main(String[] args){
		NodeServerProperties1 p1 = new NodeServerProperties1();
		p1.setNodePort(9001);
//		ZxId id1 = new ZxId(3, 0);
//		List<String> retList = getDiffResponse(p1,id1 );
//		
//		System.out.println(retList);
		fillDataInProperties(p1);
		
	}
	
}
