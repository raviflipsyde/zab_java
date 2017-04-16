package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.input.ReversedLinesFileReader;

import servers.NodeServerProperties1;
import servers.ZxId;

public class FileOps {

	
	
	public static void readHistory(NodeServerProperties1 properties) {
		
		String fileName = "CommitedHistory_" + properties.getNodePort() + ".properties";
		Map<String, String> map = properties.getDataMap();
		Properties dataMap = new Properties();
		String line = null;
		long epoch=0, counter=0;
		String key,value;
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

		properties.setCurrentEpoch(epoch);
		ZxId zxid = new ZxId(epoch, counter);
		properties.setLastZxId(zxid);

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
	
	public static String readLastLine(NodeServerProperties1 properties ){
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
			String[] arr = ret.split(",");
			epoch = Long.parseLong(arr[0].trim());
			counter = Long.parseLong(arr[1].trim());
			key = arr[2].trim();
			value = arr[3].trim();
			
			reveFileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
		
	}
	
}
