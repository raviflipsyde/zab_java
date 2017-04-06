package util;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import servers.Message;
import servers.Notification;

public class SyncDataStructs {

	private static SyncDataStructs instance = null;
	private MpscArrayQueue<Notification> electionQueue = null;
	private List<InetSocketAddress> memberList = null;
	
	private SyncDataStructs(){
		electionQueue = new MpscArrayQueue<Notification>(100);
		memberList  = new CopyOnWriteArrayList<InetSocketAddress>();
	}
	public static SyncDataStructs getInstance(){
		if(instance == null){
			instance = new SyncDataStructs();
		}
		return instance;
	}
	
	public MpscArrayQueue<Notification> getElectionQueue() {
		return getInstance().electionQueue;
	}
	
	public List<InetSocketAddress> getMemberList() {
		return getInstance().memberList;
	}
	
	
	
}
