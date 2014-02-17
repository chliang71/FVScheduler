package scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.measurement.FloodlightMeasurementInfo;

import org.flowvisor.measurement.MeasurementInfo;
import org.flowvisor.measurement.MeasurementInfo.SwitchEntry;
import org.flowvisor.measurement.MeasurementRMI;
import org.openflow.protocol.OFMatch;

import com.google.gson.Gson;

import randy.Programming;
import randy.model.ModelInput;
import randy.model.ModelOutput;


public class Scheduler {

	boolean singleNode;
	//HashMap<String, SliceStats> globalSliceStatsMap;	
	HashMap<String, SliceStats> sliceStatsMap;
	HashMap<String, SliceInfo> sliceInfoMap;
	HashMap<String, SwitchStats> switchStats;
	HashMap<String, SwitchStats> lastSwitchStats;//just a record
	
	HashMap<String, LinkedList<FlowInfo>> switchFlowMap; //current switch flowtable

	//the index for LP part
	LinkedList<String> sliceIndex;//slice name
	LinkedList<String> fsIndex;//dpid

	HashMap<String, String> switchControllerMap;  //maintains the mapping from switch to controller 

	String currentMarshalledConsumption;

	Switches switches;
	boolean toStop;

	FlowSpaceInfo flowspaceInfo;
	ConsumptionModel cm;

	SocketChannel sc;
	
	FloodlightMeasurementManager floodlightMeasurement;
	FlowvisorMeasurementManager flowvisorMeasurement;
	
	SwitchMeasurementManager switchMeasurementManager;
	
	ArrayList<Long> edgeSwitches;

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
	private final String ALL_SLICE_QUERY = "fvctl -f /dev/null list-slices ";
	private final String ALL_SLICE_QUERY2 = "./fvctljson.py -f /dev/null list-slices ";
	
	private final String QUERY_FLOW_TABLE = "fvctl -f /dev/null list-datapath-flowdb ";

	public static void main(String[] args) {
		Scheduler s = new Scheduler(true);
		//s.testFlowvisor();
		s.run();
		//s.testMeasurement();
		//s.floodlightMeasurement.readRMIConfigFromFile("/home/chen/floodlightRMIConfig");
	}
	
	public Scheduler(boolean singleNodeMode) {
		edgeSwitches = new ArrayList<Long>();
		sliceStatsMap = new HashMap<String, SliceStats>();
		lastSwitchStats = new HashMap<String, SwitchStats>();
		sliceInfoMap = new HashMap<String, SliceInfo>();
		switchStats = new HashMap<String, SwitchStats>();
		switchControllerMap = new HashMap<String, String>();
		sliceIndex = new LinkedList<String>();
		switches = null;
		flowspaceInfo = null;
		toStop = false;
		currentMarshalledConsumption = "";
		singleNode = singleNodeMode;
		switchFlowMap = new HashMap<String, LinkedList<FlowInfo>>();
		floodlightMeasurement = new FloodlightMeasurementManager();
		flowvisorMeasurement = new FlowvisorMeasurementManager();
		switchMeasurementManager = new SwitchMeasurementManager();
	}


	public void testFlowvisor() {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry("localhost", 44444);
			MeasurementRMI server = 
					(MeasurementRMI) registry.lookup("flowvisor_measurement");
			
			while(true) {
				System.out.println("----------------------------->");
				MeasurementInfo info = server.getCurrentMeasurementInfoAndRefresh();
				/*System.out.println("match:" + info.getMatchTimeMap().keySet());
				System.out.println("current:" + info.getCurrentFlowCount().keySet());
				System.out.println("last:" + info.getLastFlowCount().keySet());*/
				ConcurrentHashMap<OFMatch, SwitchEntry> map = info.getMatchTimeMap();
				for(OFMatch match : map.keySet()) {
					SwitchEntry entry = map.get(match);
					System.out.println(match.getNetworkSource() + "--->" + match);
					if(match.getNetworkSource() != 0 && match.getNetworkSource() != -1) {
						// =0 is 0.0.0.0 =-1 is 255.255.255.255,can not be host!
						// otherwise, assume it's a host and make the switch an edge
						edgeSwitches.add(entry.getSwid());
					} else {
						System.out.println("seeing strange match! with net source:" + match.getNetworkSource() + ":" + match);
					}
				}
				
				for(Long swid : edgeSwitches) {
					System.out.println("edge!----->" + swid);
				}
				edgeSwitches.clear();
				Thread.sleep(5000);
			}
		} catch (RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testMeasurement() {
		//querySwitches();
		floodlightMeasurement.addNewRMI("localhost", 40001, "localcontroller1");
		System.out.println("entering loop");
		while(true) {
			HashMap<Long, Double> fractions = floodlightMeasurement.getAllRMIFraction();
			if(fractions == null)
				System.out.println("Nothing returned!");
			else {
				for(Long swid : fractions.keySet()) {
					System.out.println("for sw:" + swid + " its consumption is " + fractions.get(swid));
				}
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void testMeasurement2() {
		//querySwitches();
		floodlightMeasurement.addNewRMI("localhost", 40001, "localcontroller1");
		System.out.println("entering loop");
		while(true) {
			HashMap<Long, Double> fractions = floodlightMeasurement.getAllRMIFraction();
			if(fractions == null)
				System.out.println("Nothing returned!");
			else {
				for(Long swid : fractions.keySet()) {
					System.out.println("for sw:" + swid + " its consumption is " + fractions.get(swid));
				}
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void test() {
		querySwitches();
		queryFlowTableAll();
		for(String dpid : switchFlowMap.keySet()) {
			LinkedList<FlowInfo> infos = switchFlowMap.get(dpid);
			for(FlowInfo info : infos) {
				System.out.println("for:" + dpid + ":" + info);
			}
		}
		ArrayList<String> dpids = 
				new ArrayList<String>(switchFlowMap.keySet());
		for(int i = 0;i<dpids.size() - 1;i++) {
			String dpid1 = dpids.get(i);
			String dpid2 = dpids.get(i + 1);
			System.out.println("------>for " + dpid1 
					+ " " + dpid2 
					+ " " + compareSwitchFlowTable(dpid1, dpid2));
		}
		String dpidf = dpids.get(0);
		String dpidl = dpids.get(dpids.size() - 1);
		System.out.print("------>for " + dpidf 
					+ " " + dpidl 
					+ " " + compareSwitchFlowTable(dpidf, dpidl));
		//queryFlowTable("1");
		//sched.queryAllSliceInfo();
		//		System.out.println("start:" + time());
		//		//querySliceStats("upper");
		//		//querySliceInfo("upper");
		//		querySwitches();
		//		System.out.println(switches);
		//		querySliceStats("first");
		//		querySliceStats("second");
		//		//createSlice("test", "tcp:127.0.0.1:10001", null);
		//		//createSlice("test2", "tcp:127.0.0.1:10002", null);
		//		//System.out.println("after creating 2 slices:" + time());
		//		//flowspaceInfo.display();
		//		//createFlowSpace(new Long(1), "upper");
		//		//removeFlowSpace(new Long(1));
		//		//createFlowspaceForAll("test");;
		//		//System.out.println("after creating 4 flowspaces:" + time());
		//		/*try {
		//			Thread.currentThread().sleep(2000);
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}*/
		//		//System.out.println("after waiting for 2000 ms:" + time());
		//		//migrateFlowSpace("test2", "dpid1");
		//		//queryFlowspcae();
		//		//flowspaceInfo.display();
		//		//querySwitchStats(new Long(1));
		//		//removeSlice("test");
		//		//System.out.println("after migrating 1 flowspace");
		//		//System.out.println("done:" + time());
		//		//removeSlice("test");
		//		//removeSlice("test2");
	}

	public void run() {
		//1.establish the info about the network, switch id, controller id,url,etc.
		//2.periodically checks the stats of each controller, all info we need is slice stats, pass it the LP
		//3.wait for LP to response, apply the update using flowspace update
		//we should have known the url of all controllers at this point, thus we
		//can create slice accordingly. And switches can be obtained by query

		//We assume that before the scheduler starts, there already exist a bunch of flowspace
		//and controllers, we only change them, not create them(this makes debug a little easier)
		querySwitches();
		queryFlowspcae();
		queryAllSliceInfo();
		cm = new StaticConsumptionModel();
		
		floodlightMeasurement.readRMIConfigFromFile("/home/chen/floodlightRMIConfig");	
		flowvisorMeasurement.initRMI("flowvisor_measurement", "localhost", 44444);
		
		flowspaceInfo.display();
		System.out.println("# of controller:" + getSliceNumber());
		System.out.println("their slice are:");
		for(String s : sliceIndex) {
			System.out.println(s + " ");
		}
		System.out.println("# of switch:" + getSwitchNumber());
		System.out.println("their flowspace are:");
		for(String s : fsIndex) {
			System.out.println(s + " ");
		}
		//createFlowspaceForAll(null);//this will create flowspaces for all switches, all flowspace is under slice "fvadmin"

		//TODO:create slices for all controllers, should be easy, given the url of all controllers 

		//for now, let's assume the total number of controllers and switches are constant
		ModelInput input = staticConfig();
		//////////////////
		if(!singleNode) {
			try{
				sc = SocketChannel.open();
				sc.connect(new InetSocketAddress(InetAddress.getByName("localhost"), 12345));
				sc.configureBlocking(false);
				while(!sc.finishConnect());
			} catch(Exception e) {
				e.printStackTrace();
				System.err.println("ERROR connecting central scheduler, quiting");
				System.exit(1);
			}
		}
		
		//the main schedule loop, do the 2nd,3rd things mentioned above
		while(!toStop) {
			ArrayList<String> allDpid = switches.getAllDPID();
			for(String dpid : allDpid) {
				querySwitchStats(dpid);
			}
			//displaySwStats();
			int[][] consumption = generateConsumption();			
			if(!singleNode) {
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				String marshalledConsupmtion = marshalConsumption(consumption); 
				System.out.println(marshalledConsupmtion);
				try {
					ByteBuffer sendbuffer = ByteBuffer.wrap(marshalledConsupmtion.getBytes());
					while(sendbuffer.hasRemaining()) {
						sc.write(sendbuffer);
					}
					sc.read(buffer);
					buffer.flip();
					String ret = new String(buffer.array());
					ret = ret.trim();
					String last = ret;
					if(ret.contains(" ")) {
						String[] sub = ret.split(" ");
						last = sub[sub.length - 1];
					}
					System.out.println("got this: " + ret + "| last:" + last + "|");
					int[] assignment = new Gson().fromJson(last, int[].class);
					for(int i = 0;i<assignment.length;i++) {
						System.out.print(assignment[i] + " ");
					}
					System.out.println();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {

				/*for (int i = 0;i<fsIndex.size();i++) {
					for(int j=0;j<sliceIndex.size();j++) {
						System.out.print(consumption[i][j] + " ");
					}				
					System.out.println();
				}*/
				/*int[][] invertedConsumption = new int[consumption[0].length][consumption.length];
				for(int i = 0;i<consumption.length;i++)
					for(int j = 0;j<consumption[0].length;j++)
						invertedConsumption[j][i] = consumption[i][j];*/
				
				/*HashMap<Long, Double> fractions = floodlightMeasurement.getAllRMIFraction();
				if(fractions == null)
					System.out.println("Nothing returned!");
				else {
					System.out.println("==============>>>>");
					for(Long swid : fractions.keySet()) {
						System.out.println("for sw:" + swid + " its consumption is " + fractions.get(swid));
					}
				}*/

				/*for(long swid : this.switches.getAllDPIDinLong()) {
					switchMeasurementManager.addNewInfo(swid);
				}*/
				
				ConcurrentHashMap<String, FloodlightMeasurementInfo> floodlightInfo = floodlightMeasurement.getAllRMIInfo();
				if(floodlightInfo.size() == 0) {
					System.out.println("No floodlight Info!!");
				} else {
					System.out.println("received info from floodlight");
					for(String id : floodlightInfo.keySet()) {
						FloodlightMeasurementInfo info = floodlightInfo.get(id);
						switchMeasurementManager.addSwitchNonEventTime(id, info.getNonHandlerTime());
						System.out.println("++++++++++++++++++++++++++non time:" + info.getNonHandlerTime());
						HashMap<Long, Double> fraction = info.getHandlerFraction();
						ArrayList<Long> switches = info.getAllSwitch();
												
						System.out.println("for controller:" + id + " faction:" + fraction.size() + " sw:" + switches + " totalNonHandler:" + info.getNonHandlerTime());
						String s = "";
						for(Long swid : switches) {
							
							switchMeasurementManager.addNewInfo(swid);
							
							switchMeasurementManager.addSwitchToController(id, swid);							s += swid + ":";
							if(fraction.containsKey(swid)) {
								s += fraction.get(swid) + " ";
								switchMeasurementManager.addFraction(swid, fraction.get(swid));
							} else {
								s += "null" + " ";
								switchMeasurementManager.addFraction(swid, 0);
							}
						}
						//System.out.println(s);
					}					
				}
				
				MeasurementInfo flowvisorInfo = flowvisorMeasurement.getFlowvisorInfo();
				edgeSwitches.clear();
				if(flowvisorInfo == null) {
					System.out.println("No flowvisor info!");
				} else {
					System.out.println("received info from flowvisor");
					ConcurrentHashMap<OFMatch, SwitchEntry> map = flowvisorInfo.getMatchTimeMap();
					for(OFMatch match : map.keySet()) {
						SwitchEntry entry = map.get(match);
						System.out.println(match.getNetworkSource() + "--->" + match);
						if(match.getNetworkSource() != 0 && match.getNetworkSource() != -1) {
							// =0 is 0.0.0.0 =-1 is 255.255.255.255,can not be host!
							// otherwise, assume it's a host and make the switch an edge
							edgeSwitches.add(entry.getSwid());
							switchMeasurementManager.setEdge(entry.getSwid(), true);
						} else {
							System.out.println("seeing unexpected match! with net source:" + match.getNetworkSource() + ":" + match);
						}
					}
					ConcurrentHashMap<Long, Long> flowCount = flowvisorInfo.getCurrentFlowCount();
					for(Long swid : flowCount.keySet()) {
						System.out.println(swid + "f count-->" + flowCount.get(swid));
					}
				}				
				ConcurrentHashMap<Long, Long> currentFlowCount = flowvisorInfo.getCurrentFlowCount();
				ConcurrentHashMap<Long, Long> lastFlowCount = flowvisorInfo.getLastFlowCount();
				
				//at this point, info from flowvisor and floodlight are
				//both available, now combine them
				for(String conid : floodlightInfo.keySet()) {
					FloodlightMeasurementInfo coninfo = floodlightInfo.get(conid);
					ArrayList<Long> allswitches = coninfo.getAllSwitch();
					System.out.println("for controller===============>" + conid + " switches are:" + allswitches);
					for(Long swid : allswitches) {
						String s = "for swid:" + swid + ":";
						if(!currentFlowCount.containsKey(swid)) {
							s += "do not have current flow count info for:" + swid;
							switchMeasurementManager.addCurrCount(swid, (long)0);
						} else {
							s += "current flow:" + currentFlowCount.get(swid);
							switchMeasurementManager.addCurrCount(swid, currentFlowCount.get(swid));
						}
						s += "---->";
						
						if(!lastFlowCount.containsKey(swid)) {
							s += "do not have last flow count info for:" + swid;
							switchMeasurementManager.addLastCount(swid, (long)0);
						} else {
							s += "last flow:" + lastFlowCount.get(swid);
							switchMeasurementManager.addLastCount(swid, lastFlowCount.get(swid));
						}
						s += " edge?:";
						
						if(edgeSwitches.contains(swid)) {
							s += "an edge switch!";
						} else {
							s += "a core switch!";
						}						
						System.out.println(s);
					}
				}
				
				String summaryString = switchMeasurementManager.displayInfo();
				System.out.println(summaryString);

				switchMeasurementManager.constructEquationInfo();
				
				//input.setConsumpiton(consumption);
				/*
				ModelOutput output = Programming.runProgramming(input, 1);
				if(output == null) {
					System.out.println("LP returned:No solution found!");
				} else {
					System.out.println("LP returned:Attempt to update");
					int[] assignment = output.getControllerSwitchAssignment();
					updateMapping(assignment);
				}*/
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private int[][] getLatency() {
		int rows = getSliceNumber();
		int cols = getSwitchNumber();
		int[][] latency = new int[getSliceNumber()][getSwitchNumber()];
		//let's make it simple for now
		for(int i = 0;i<rows;i++) 
			for(int j=0;j<cols;j++)
				latency[i][j] = 1;
		return latency;
	}
	
	private int getAverageLatency() {
		int[][] latency = getLatency();
		int sum = 0;
		int rows = latency.length;
		int cols = latency[0].length;
		for(int i = 0;i<rows;i++)
			for(int j = 0;j<cols;j++)
				sum += latency[i][j];		
		return sum/(rows*cols); 
	}
	
	private int getWorstLatency() {
		//let's make it simple for now
		return getAverageLatency() + 1;
	}
	
	private int[][] getCapacities() {
		int rows = getSliceNumber();
		int cols = getCapacitiesNumber();
		int[][] caps = new int[getSliceNumber()][getCapacitiesNumber()];
		//let's make it simple for now
		for(int i = 0;i<rows;i++) {
			for(int j = 0;j<cols;j++) {
				caps[i][j] = 25;
			}
		}
		return caps;
	}
	
	private int getCapacitiesNumber() {
		return 2; //CPU?RAM?
	}
	
	private int[][] getMigrationCost() {
		int rows = getControllerNumber();
		int cols = getLocationNumber();
		int[][] cost = new int[rows][cols];
		for(int i = 0;i<rows;i++)
			for(int j = 0;j<cols;j++)
				cost[i][j] = 1;
		return cost;
	}
	
	private int getControllerNumber() {
		return getSliceNumber();
	}
	
	private int getLocationNumber() {
		return getSliceNumber();
	}
	
	public ModelInput staticConfig() {
		//set the parameters that are constant over time, including:
		//# of controller
		//# of switch
		//latency(avg and worst)
		//capacity of controller
		//location number
		//migration cost
		ModelInput input = new ModelInput();
		//for now, let's say we are free to user all controllers
		input.setControllerNumber(getControllerNumber());
		input.setSwitchNumber(getSwitchNumber());		
		input.setCapacitiesNumber(getCapacitiesNumber());//CPU?RAM?
		//these is the number of all controllers, but some of them may be idle
		input.setLocationNumber(getLocationNumber()); 
		input.setLatency(getLatency());		
		input.setCapacities(getCapacities());
		input.setMigrationCost(getMigrationCost());
		input.setAverageLatency(getAverageLatency());
		input.setWorstLatency(getWorstLatency());
		return input;
	}

	public void stop() {
		toStop = true;
	}

	public int getSliceNumber() {
		return this.sliceInfoMap.keySet().size();
	}

	public int getSwitchNumber() {
		return this.switches.getSwitchNumber();
	}

	private String runCmd(String cmd) {		
		String ret = null;
		do {
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
			if(ret.startsWith("Could not reach a FlowVisor RPC")) {
				System.out.println("cmd failed, try again");
			}
		} while(ret.startsWith("Could not reach a FlowVisor RPC"));
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
	
	public void queryAllSliceInfo() {
		String cmd = ALL_SLICE_QUERY;
		String ret = runCmd(cmd);
		if(!ret.startsWith("Configured slices:")) {
			System.err.println("ERROR unexpected string! " + ret);
			return;
		}
		ret = ret.substring("Configured slices:".length() + 1);
		String[] substrings = ret.split("\n");
		for(String substring : substrings) {
			String[] subsubstrings = substring.split("-->");
			System.out.println(subsubstrings[0].trim() + ":" + subsubstrings[1].trim() + ";");
			if(subsubstrings[1].trim().equals("enabled") && !subsubstrings[0].trim().equals("fvadmin"))
				sliceIndex.add(subsubstrings[0].trim());
		}
		for(String sname : sliceIndex) {
			if(sname.equals("fvadmin"))
				continue;
			querySliceInfo(sname);
		}
	}

	public void querySliceInfo(String sliceName) {
		String cmd = SLICE_INFO_QUERY + sliceName;
		String redundant = "Note: No switches connected; no runtime stats available";
		String ret = runCmd(cmd);
		if (ret == null) {
			System.err.print(cmd + " Execution Failed!!");
			return;
		}
		if (ret.startsWith(redundant)) {
			ret = ret.substring(redundant.length() + 1);
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
		switchControllerMap = flowspaceInfo.getSwitchControllerMap();
		fsIndex = new LinkedList<String>(flowspaceInfo.getAllFSDPID());
	}

	public boolean migrateFlowSpace(String sliceName, String flowspaceName) {
		if(flowspaceInfo.confimMapping(sliceName, flowspaceName)) {
			System.out.println("Updating:Already existing mapping:" + flowspaceName + "-->" + sliceName);
			return true;//no need to do anything
		}
		System.out.println("Updating:Updating!" + flowspaceName + "-->" + sliceName);
		boolean ret = updateFlowSpace2(sliceName, flowspaceName);
		if (ret == false) 
			return false;
		flowspaceInfo.changeFlowspace(sliceName, flowspaceName);
		return true;
	}

	private boolean updateFlowSpace2(String sliceName, String fsName) {
		//delete and then create
		String cmd = REMOVE_FLOWSPACE + fsName;
		String ret = runCmd(cmd);
		if(ret == null) {
			System.err.println(cmd + " Execution Failed!!");
			return false;			
		}
		if(!ret.startsWith("Flowspace entries have been removed.")) {
			System.err.println("Update failed, " + ret);
			return false;			
		}
		if(!createFlowSpace(flowspaceInfo.lookupDpidByFSName(fsName), sliceName, fsName, false)) {
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("unused")
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

	//For the following 2 method, one thing is that each switch
	//will only have one flowspace. It is because we need exactly one
	//controller to control all things of the switch, and in this
	//case, one flowspace for the switch suffice
	public boolean createFlowSpace(String dpid, String sliceName, String fsName, boolean update) {
		String name;
		if(fsName ==null)
			name = dpid;
		else
			name = fsName;
		String cmd = CREATE_FLOWSPACE + name //name of the fs
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
			System.err.println("NOTE return null when creating flowspace:" + dpid);		
		} else if(!ret.startsWith("FlowSpace " + name + " was added")) {
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
		ArrayList<String> dpids = switches.getAllDPID();
		for(String dpid : dpids) {
			createFlowSpace(dpid, sliceName, null, false);
		}
		queryFlowspcae();
	}

	public void removeFlowSpace(Long dpid) {
		String cmd = REMOVE_FLOWSPACE + dpid;
		//System.out.println(cmd);
		String ret = runCmd(cmd);
		//System.out.println(ret);
		if(!ret.equals("Flowspace entries have been removed.")) {
			System.err.println("ERROR removing Flowspace:" + ret);
			return;
		}
		queryFlowspcae();
	}

	public void querySwitchStats(String dpid) {
		String cmd = SWITCH_STATS_QUERY + dpid;
		//System.out.println(cmd);
		String ret = runCmd(cmd);
		SwitchStats stat = new SwitchStats();
		stat.parseFromCmd(ret);
		//switchStats.put(dpid, stat);				
		if(lastSwitchStats.containsKey(dpid)) {
			SwitchStats last = lastSwitchStats.get(dpid);
			SwitchStats inc = new SwitchStats();
			//inc = stat - last
			inc = stat.substract(last);
			switchStats.put(dpid, inc);
		} else {
			switchStats.put(dpid, stat);
		}
		lastSwitchStats.put(dpid, stat);		
	}

	public int compareSwitchFlowTable(String dpid1, String dpid2) {
		if(!switchFlowMap.containsKey(dpid1)) {
			System.out.println("on info for switch:" + dpid1);
			return 0;
		}
		if(!switchFlowMap.containsKey(dpid2)) {
			System.out.println("on info for switch:" + dpid2);
			return 0;
		}
		int score = 0;
		LinkedList<FlowInfo> flowsinfos1 = switchFlowMap.get(dpid1);
		LinkedList<FlowInfo> flowsinfos2 = switchFlowMap.get(dpid2);
		for(FlowInfo finfo1 : flowsinfos1) {
			for(FlowInfo finfo2 : flowsinfos2) {
				if(finfo1.equals(finfo2))
					score ++;
			}
		}
		return score;
	}
	
	public void queryFlowTableAll() {
		ArrayList<String> dpids = switches.getAllDPID();
		for(String dpid : dpids) {
			System.out.println("querying:" + dpid);
			queryFlowTable(dpid);
		}
	}
	
	public void queryFlowTable(String dpid) {
		String cmd = QUERY_FLOW_TABLE + dpid;
		String ret = runCmd(cmd);
		String redundant = "Flows seen at FlowVisor:";

		LinkedList<FlowInfo> allFlows;
		if(!switchFlowMap.containsKey(dpid)) {
			allFlows = new LinkedList<FlowInfo>();
		} else {
			allFlows = switchFlowMap.get(dpid);
		}
		
		if(ret.startsWith(redundant)) {
			ret = ret.substring(redundant.length() + 1);
			ret = ret.replaceAll("u'", "\"");
			ret = ret.replaceAll("'", "\"");
			ret = ret.trim();
			String[] rets = ret.split("\n");
			for(String s : rets) {				
				FlowInfo finfo = new FlowInfo();
				finfo.parseFromJson(s);
				if(!allFlows.contains(finfo))
					allFlows.add(finfo);
			}
		}
		switchFlowMap.put(dpid, allFlows);
		System.out.println("====================>" + allFlows.size());
	}
	
	public int[][] generateConsumption() {
		//generate resource consumption based on current switchStats
		int slicenum = sliceIndex.size();
		int fsnum = fsIndex.size();
		int[][] consumption = new int[fsnum][slicenum];
		for(int i = 0;i<fsnum;i++) {
			for(int j = 0;j<slicenum;j++) {
				consumption[i][j] = 0;
			}
		}		
		ArrayList<String> allDpid = new ArrayList<String>(switchStats.keySet());
		for(String dpid : allDpid) {
			SwitchStats ss = switchStats.get(dpid);
			int currConsumption = ss.generateConsumption(cm);

			int currSWIndex = fsIndex.indexOf(dpid);
			String sliceName = flowspaceInfo.lookupSliceByDPID(dpid);
			int currSliceIndex = sliceIndex.indexOf(sliceName);
			
			//System.out.println("slice:" + sliceName + "-->switch:" + dpid);
			
			consumption[currSWIndex][currSliceIndex] = currConsumption;
		}		
		return consumption;		
	}

	public String marshalConsumption(int[][] consumption) {
		Gson gson = new Gson();
		String jstring = gson.toJson(consumption);
		return jstring;
	}

	public void updateMapping(int[] mapping) {
		//update switch -> controller mapping
		//mapping[i] = j means switch i now should be assigned to controller j
		if(mapping == null) 
			return;
		for(int i = 0;i<mapping.length;i++) {
			String dpid = fsIndex.get(i);
			String sliceName = sliceIndex.get(mapping[i]);
			String flowspaceName = flowspaceInfo.lookupFSNameByDpid(dpid);
			migrateFlowSpace(sliceName, flowspaceName);
		}
	}
}
