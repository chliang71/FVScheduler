package scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Scheduler {
	
	ConcurrentHashMap<String, SliceStats> sliceStatsMap;
	ConcurrentHashMap<String, SliceInfo> sliceInfoMap;
	ConcurrentHashMap<Long, SwitchStats> switchStats;
	
	Switches switches;
	
	
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
		//s.querySliceStats("upper");
		//s.querySliceInfo("upper");
		s.querySwitches();
		//System.out.println(s.switches);
		//s.createSlice("test", "tcp:127.0.0.1:10001", null);
		//s.removeSlice("test");
		//s.queryFlowspcae();
		//s.migrateFlowSpace("lower", "00:00:00:00:00:00:00:03");
		//s.flowspaceInfo.display();
		//s.createFlowSpace(new Long(1), "upper");
		//s.removeFlowSpace(new Long(1));
		//s.createFlowspaceForAll("test");;
		//s.queryFlowspcae();
		//s.flowspaceInfo.display();
		s.querySwitchStats(new Long(1));
	}
	
	public Scheduler() {
		sliceStatsMap = new ConcurrentHashMap<String, SliceStats>();
		sliceInfoMap = new ConcurrentHashMap<String, SliceInfo>();
		switchStats = new ConcurrentHashMap<Long, SwitchStats>();
		switches = null;
		flowspaceInfo = null;
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
			ret = line + '\n';
			while (line != null) {
				line = reader.readLine();
				if(line != null)
					ret += line + '\n';
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
			System.out.println("Problem while creating slice:" + ret);
			return false;
		}
	}
	
	public boolean removeSlice(String sliceName) {
		String cmd = REMOVE_SLICE + sliceName;
		String ret = runCmd(cmd).trim();
		if (ret.equals("Slice " + sliceName + " has been deleted")) {
			return true;
		} else {
			System.out.println("Problem while removing slice:" + ret);
			return false;
		}
	}
	
	public void querySliceInfo(String sliceName) {
		String cmd = SLICE_INFO_QUERY + sliceName;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.out.print(cmd + " Execution Failed!!");
			return;
		}
		SliceInfo info = new SliceInfo();
		info.parseFromJson(ret);
		System.out.println(info);
		sliceInfoMap.put(sliceName, info);
	}
	
	public void querySliceStats(String sliceName) {
		String cmd = SLICE_STAT_QUERY + sliceName;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.out.print(cmd + " Execution Failed!!");
			return;
		}
		if (ret.trim().startsWith("Internal Error")) {
			System.out.println(ret);
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
			System.out.print(cmd + " Execution Failed!!");
			return;
		}
		if (switches != null) {
			System.out.println("Updating Switch Info");
		}
		switches = new Switches();
		switches.parseFromCmd(ret);
	}
	
	public void queryFlowspcae() {
		String cmd = FLOWSPACE_QUERY;
		String ret = runCmd(cmd);
		if (ret == null) {
			System.out.print(cmd + " Execution Failed!!");
			return;
		}
		if (flowspaceInfo != null) {
			System.out.println("Updating Flowspace Info");
		}
		flowspaceInfo = new FlowSpaceInfo();
		flowspaceInfo.createFromCmd(ret);
	}
	
	public boolean migrateFlowSpace(String sliceName, String dpid) {
		//This method changes the slice-action of a slice
		//this will delete the slice-action of current slice
		//and ad action of another slice, thus migrating
		//the flowspace
		String name = flowspaceInfo.lookUpNamebyDPID(dpid);
		if (name == null){
			System.out.println("Nonexisting flowspace");
			return false;
		}
		boolean ret = updateFlowSpace(sliceName, name);
		if (ret == false) 
			return false;
		//two ways to update FlowSpaceInfo variable, manually or call fvctl again 
		//for now, do it manually.
		flowspaceInfo.changeFlowspace(sliceName, name);
		return true;		
	}
	
	private boolean updateFlowSpace(String sliceName, String flowspaceName) {
		//only change slice-actions, =7 means that the slice has the full control 
		// over the flowspace
		String cmd = UPDATE_FLOWSPACE + " -s " + sliceName + "=7 " + flowspaceName;
		System.out.println(cmd);
		String ret = runCmd(cmd);
		if (ret == null) {
			System.out.println(cmd + " Execution Failed!!");
			return false;
		} 
		if (!ret.startsWith("Flowspace " + flowspaceName + " was updated")) {
			System.out.println("Update failed, " + ret);
			return false;
		}
		return true;
	}
	
	//For the following 2 method, one thing is that each switch
	//will only have one flowspace. It is because we need exactly one
	//controller to control all things of the switch, and in this
	//case, one flowspace for the switch suffice
	public boolean createFlowSpace(Long dpid, String sliceName, boolean update) {
		String cmd = CREATE_FLOWSPACE + "dpid" + dpid.toString() //name of the fs
				+ " " + dpid //dpid of this fs
				+ " 1 "  //priority of this fs(all shall be 1)
				+ " any " //match of this fs
				+ sliceName + "=7"; //slice of this fs
		System.out.println(cmd);
		String ret = runCmd(cmd);
		System.out.println(ret);
		if(!ret.startsWith("FlowSpace dpid" + dpid + " was added")) {
			System.out.println("ERROR:" + ret);
			return false;
		}

		if(update == true) {
			//update flowspaceInfo, calling quertFlowspace might seem expensive
			//but I think it's find because creating flowspace should only be called
			//when a new switch comes in, and the scheduler starts up and establishes
			//flowspaces
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
		String cmd = REMOVE_FLOWSPACE + "dpid" + dpid.toString();
		System.out.println(cmd);
		String ret = runCmd(cmd);
		System.out.println(ret);
		if(!ret.equals("Flowspace entries have been removed.")) {
			System.out.println("ERROR:" + ret);
			return;
		}
		
		queryFlowspcae();
	}
	
	public void querySwitchStats(Long dpid) {
		String cmd = SWITCH_STATS_QUERY + dpid;
		System.out.println(cmd);
		String ret = runCmd(cmd);
		SwitchStats stat = new SwitchStats();
		stat.parseFromCmd(ret);
		switchStats.put(dpid, stat);
	}
}
