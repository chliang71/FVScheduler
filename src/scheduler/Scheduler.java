package scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Scheduler {
	
	//HashMap<String, SliceStats> globalSliceStatsMap;	
	HashMap<String, SliceStats> sliceStatsMap;
	HashMap<String, SliceInfo> sliceInfoMap;
	HashMap<Long, SwitchStats> switchStats;
	
	HashMap<Long, String> switchControllerMap;  //maintains the mapping from switch to controller 
	
	Switches switches;
	boolean toStop;
	
	FlowSpaceInfo flowspaceInfo;
	
	private final String SLICE_STAT_QUERY = "fvctl -f /dev/null list-slice-stats ";
	private final String SLICE_INFO_QUERY = "fvctl -f /dev/null list-slice-info ";
	private final String SWITCH_QUERY = "fvctl -f /dev/null list-datapaths";
	private final String CREATE_SLICE = "fvctl -f /dev/null add-slice -p='\\n' ";
	private final String REMOVE_SLICE = "fvctl -f /dev/null remove-slice ";
	private final String FLOWSPACE_QUERY = "fvctl -f /dev/null list-flowspace";
	private final String CREATE_FLOWSPACE = "fvctl -f /dev/null add-flowspace ";
	private final String REMOVE_FLOWSPACE = "fvctl -f /dev/null remove-flowspace ";
	private final String UPDATE_FLOWSPACE = "fvctl -f /dev/null update-flowspace ";
	private final String SWITCH_STATS_QUERY = "fvctl -f /dev/null list-datapath-stats ";
	
	public static void main(String[] args) {
		Scheduler s = new Scheduler();
		s.test();
	}
	
	
	private String time() {
		return System.currentTimeMillis()/1000 + ":" + System.currentTimeMillis()%1000;
	}
	
	public void test() {
		System.out.println("start:" + time());
		//querySliceStats("upper");
		//querySliceInfo("upper");
		querySwitches();
		//System.out.println(s.switches);
		createSlice("test", "tcp:127.0.0.1:10001", null);
		createSlice("test2", "tcp:127.0.0.1:10002", null);
		System.out.println("after creating 2 slices:" + time());
		//flowspaceInfo.display();
		//createFlowSpace(new Long(1), "upper");
		//removeFlowSpace(new Long(1));
		createFlowspaceForAll("test");;
		System.out.println("after creating 4 flowspaces:" + time());
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("after waiting for 2000 ms:" + time());
		migrateFlowSpace("test2", "dpid1");
		//queryFlowspcae();
		//flowspaceInfo.display();
		//querySwitchStats(new Long(1));
		//removeSlice("test");
		System.out.println("after migrating 1 flowspace");
		System.out.println("done:" + time());
		//removeSlice("test");
		//removeSlice("test2");
	}
	
	public void run() {
		//1.establish the info about the network, switch id, controller id,url,etc.
		//2.periodically checks the stats of each controller, all info we need is slice stats, pass it the LP
		//3.wait for LP to response, apply the update using flowspace update
		//we should have known the url of all controllers at this point, thus we
		//can create slice accordingly. And switches can be obtained by query
		querySwitches();
		createFlowspaceForAll(null);//this will create flowspaces for all switches, all flowspace is under slice "fvadmin"
		
		//TODO:create slices for all controllers, should be easy, given the url of all controllers 
		
		//////////////////
		//the main schedule loop, do the 2nd,3rd things mentioned above
		while(!toStop) {
			
		}
	}
	
	public Scheduler() {
		sliceStatsMap = new HashMap<String, SliceStats>();
		sliceInfoMap = new HashMap<String, SliceInfo>();
		switchStats = new HashMap<Long, SwitchStats>();
		switchControllerMap = new HashMap<Long, String>();
		switches = null;
		flowspaceInfo = null;
		toStop = false;
	}
	
	public void stop() {
		toStop = true;
	}
	
	private String runCmd(String cmd) {		
		String ret = null;
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			
			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(
							p.getInputStream()));
			String line = reader.readLine();
			if(line != null)
				ret = line + '\n';
			while (line != null) {
				line = reader.readLine();
				if(line != null)
					ret += line + '\n';
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return ret;
	}
	
	public boolean createSlice(String sliceName, String controllerUrl, String contact) {
		if(contact == null) 
			contact = "a@a";
		String cmd = CREATE_SLICE + sliceName + " " + controllerUrl + " " + contact;
		String ret = runCmd(cmd).trim();
		if (ret.equals("Slice " + sliceName + " was successfully created")) {
			return true;
		} else {
			System.err.println("ERROR creating slice:" + ret);
			return false;
		}
	}
	
	public boolean removeSlice(String sliceName) {
		String cmd = REMOVE_SLICE + sliceName;
		String ret = runCmd(cmd).trim();
		if (ret.equals("Slice " + sliceName + " has been deleted")) {
			return true;
		} else {
			System.err.println("ERROR removing slice:" + ret);
			return false;
		}
	}
	
	public void querySliceInfo(String sliceName) {
		String cmd = SLICE_INFO_QUERY + sliceName;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.err.print(cmd + " Execution Failed!!");
			return;
		}
		SliceInfo info = new SliceInfo();
		info.parseFromJson(ret);
		//System.out.println(info);
		sliceInfoMap.put(sliceName, info);
	}
	
	public void querySliceStats(String sliceName) {
		String cmd = SLICE_STAT_QUERY + sliceName;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.err.print(cmd + " Execution Failed!!");
			return;
		}
		if (ret.trim().startsWith("Internal Error")) {
			System.err.println(ret);
			return;
		}
		SliceStats stat = new SliceStats();
		stat.parseFromJson(ret);
		sliceStatsMap.put(sliceName, stat);	
	}
	
	public void querySwitches() {
		String cmd = SWITCH_QUERY;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.err.print(cmd + " Execution Failed!!");
			return;
		}
		if (switches != null) {
			System.err.println("Updating Switch Info");
		}
		switches = new Switches();
		switches.parseFromCmd(ret);
	}
	
	public void queryFlowspcae() {
		String cmd = FLOWSPACE_QUERY;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.err.print(cmd + " Execution Failed!!");
			return;
		}
		/*if (flowspaceInfo != null) {
			System.out.println("Updating Flowspace Info");
		}*/
		flowspaceInfo = new FlowSpaceInfo();
		flowspaceInfo.createFromCmd(ret);
	}
	
	public boolean migrateFlowSpace(String sliceName, String flowspaceName) {
		boolean ret = updateFlowSpace(sliceName, flowspaceName);
		if (ret == false) 
			return false;
		flowspaceInfo.changeFlowspace(sliceName, flowspaceName);
		return true;
	}
	
	private boolean updateFlowSpace(String sliceName, String flowspaceName) {
		//only change slice-actions, =7 means that the slice has the full control 
		// over the flowspace
		String cmd = UPDATE_FLOWSPACE + " -s " + sliceName + "=7 " + flowspaceName;
		//System.out.println(cmd);
		String ret = runCmd(cmd);
		if (ret == null) {
			System.err.println(cmd + " Execution Failed!!");
			return false;
		} 
		if (!ret.startsWith("Flowspace " + flowspaceName + " was updated")) {
			System.err.println("Update failed, " + ret);
			return false;
		}
		return true;
	}
	
	private String fsName(Long dpid) {
		return "dpid" + dpid.toString();
	}
	
	//For the following 2 method, one thing is that each switch
	//will only have one flowspace. It is because we need exactly one
	//controller to control all things of the switch, and in this
	//case, one flowspace for the switch suffice
	public boolean createFlowSpace(Long dpid, String sliceName, boolean update) {
		String cmd = CREATE_FLOWSPACE + fsName(dpid) //name of the fs
				+ " " + dpid //dpid of this fs
				+ " 1 "  //priority of this fs(all shall be 1)
				+ " any " //match of this fs
				+ sliceName + "=7"; //slice of this fs
		System.out.println(cmd);
		String ret = runCmd(cmd);
		//System.out.println(ret);
		if(ret == null) {
			//Sometimes happened, ret is null but the fs is still created, wonder this 
			//is a bug of FlowVisor
			System.err.println("NOTE return null when creating flowspace:" + fsName(dpid));		
		} else if(!ret.startsWith("FlowSpace " + fsName(dpid) + " was added")) {
			System.err.println("ERROR creating flowspace:" + ret + " with " + cmd);
			return false;
		}

		if(update == true) {
			//update flowspaceInfo, calling queryFlowspace might seem expensive
			queryFlowspcae();
		}
		//update == false assumes that the update will done later
		return true;
	}
	
	public void createFlowspaceForAll(String sliceName) {
		if(sliceName == null) {
			sliceName = "fvadmin";
		}
		ArrayList<Long> dpids = switches.getAllDPID();
		for(Long dpid : dpids) {
			createFlowSpace(dpid, sliceName, false);
		}
		queryFlowspcae();
	}
	
	public void removeFlowSpace(Long dpid) {
		String cmd = REMOVE_FLOWSPACE + fsName(dpid);
		//System.out.println(cmd);
		String ret = runCmd(cmd);
		//System.out.println(ret);
		if(!ret.equals("Flowspace entries have been removed.")) {
			System.err.println("ERROR removing Flowspace:" + ret);
			return;
		}
		
		queryFlowspcae();
	}
	
	public void querySwitchStats(Long dpid) {
		String cmd = SWITCH_STATS_QUERY + dpid;
		//System.out.println(cmd);
		String ret = runCmd(cmd);
		SwitchStats stat = new SwitchStats();
		stat.parseFromCmd(ret);
		switchStats.put(dpid, stat);
	}
}
