package scheduler;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Switches {
	private ConcurrentHashMap<Long, String> switches;
	
	public Switches() {
		switches = new ConcurrentHashMap<Long, String>();
	}
	
	public ArrayList<Long> getAllDPID() {
		return new ArrayList<Long>(switches.keySet());
	}
	
	public void parseFromCmd(String string) {
		//System.out.println(string);
		String[] substring = string.split("\n");
		if (!substring[0].trim().equals("Connected switches:")) {
			System.err.println("unexpected string!");
			return;
		} 
		for (int i = 1;i<substring.length;i ++) {
			String[] elements = substring[i].trim().split(":",2);
			switches.put(Long.parseLong(elements[0].trim()), elements[1]);
		}
	}
	
	@Override
	public String toString() {
		String ret = "switches are:\n";
		for (Long key : switches.keySet()) {
			ret += "dpid:" + key + " dpid:" + switches.get(key) + '\n';
		}
		return ret;
	}
}
