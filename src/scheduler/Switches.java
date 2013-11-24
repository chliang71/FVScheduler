package scheduler;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Switches {
	private ConcurrentHashMap<String, Long> switches;
	
	public Switches() {
		switches = new ConcurrentHashMap<String, Long>();
	}
	
	public ArrayList<String> getAllDPID() {
		return new ArrayList<String>(switches.keySet());
	}
	
	public int getSwitchNumber() {
		return switches.keySet().size();
	}
	
	public void parseFromCmd(String string) {
		//System.out.println(string);
		String[] substring = string.split("\n");
		if (!substring[0].trim().equals("Connected switches:")) {
			System.err.println("unexpected string!");
			return;
		} 
		for (int i = 1;i<substring.length;i ++) {
			String[] elements = substring[i].trim().split(": ",2);
			switches.put(elements[1], Long.parseLong(elements[0].trim()));
		}
	}
	
	@Override
	public String toString() {
		String ret = "switches are:\n";
		for (String key : switches.keySet()) {
			ret += "dpid:" + key + " dpid:" + switches.get(key) + '\n';
		}
		return ret;
	}
}
