package scheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class SwitchMeasurementManager {
	/*
	 * one instance of this class represents one equation
	 * info for one equation are:
	 * for each switch:
	 *   num_old_flows, num_new_flows, edge or not
	 * 
	 * num of switch
	 * 
	 * controller non event time
	 * 
	 */
	
	class EquationInfo {
		long newEdgeFlow;
		long oldEdgeFlow;
		
		long newCoreFlow;
		long oldCoreFlow;
		
		long numSwitch;
		
		long constant;
		
		long nonhandler;
		
		public EquationInfo() {
			this.newCoreFlow = 0;
			this.newEdgeFlow = 0;
			this.oldCoreFlow = 0;
			this.oldEdgeFlow = 0;
			this.nonhandler = 0;
			this.numSwitch = 0;
		}
		
		public boolean equal(EquationInfo info) {
			return this.newCoreFlow == info.newCoreFlow && this.newEdgeFlow == info.newEdgeFlow &&
					this.oldCoreFlow == info.oldCoreFlow && this.oldEdgeFlow == info.oldEdgeFlow &&
					this.nonhandler == info.nonhandler && this.numSwitch == info.numSwitch;
		}
		@Override
		public String toString() {
			String s = "numsw:" + this.numSwitch;
			s += " oe:" + this.oldEdgeFlow + " ne:" + this.newEdgeFlow;
			s += " oc:" + this.oldCoreFlow + " nc:" + this.newCoreFlow;
			s += " nonhandler:" + this.nonhandler;
			s += "\n";
			return s;
		}
	}
	
	ConcurrentHashMap<Long, SwitchMeasurementInfo> allSwitchInfoList;
	
	ConcurrentHashMap<String, ArrayList<Long>> conSwitchMap;
	ConcurrentHashMap<String, Long> conNonEventHandler;
	
	ConcurrentHashMap<String, LinkedList<EquationInfo>> controllerEquationInfo;
	
	static private int NUM_EQUATIONS = 5;
	
	public SwitchMeasurementManager() {
		allSwitchInfoList = new ConcurrentHashMap<Long, SwitchMeasurementInfo>();
		conSwitchMap = new ConcurrentHashMap<String, ArrayList<Long>>();
		conNonEventHandler = new ConcurrentHashMap<String, Long>();
		controllerEquationInfo = new ConcurrentHashMap<String, LinkedList<EquationInfo>>();
	}
	
	public void addSwitchToController(String conid, long swid) {
		if(!conSwitchMap.containsKey(conid)) {
			ArrayList<Long> swlist = new ArrayList<Long>();
			swlist.add(swid);
			conSwitchMap.put(conid, swlist);
		} else {
			if(!conSwitchMap.get(conid).contains(swid))
				conSwitchMap.get(conid).add(swid);
		}
	}
	
	public void addSwitchNonEventTime(String conid, long time) {
		conNonEventHandler.put(conid, time);
	}
	
	public void addNewInfo(long swid) {
		SwitchMeasurementInfo info = new SwitchMeasurementInfo();
		allSwitchInfoList.put(swid, info);
	}
	
	public boolean containsSw(long swid) {
		return allSwitchInfoList.containsKey(swid);
	}
	
	public void addFraction(long swid, double fraction) {
		if(!allSwitchInfoList.containsKey(swid)) {
			System.out.println("ADD FRACTION, NO SW INFO FOR " + swid);
			return;
		}
		allSwitchInfoList.get(swid).setEventFraction(fraction);
	}

	public void addLastCount(long swid, long lastCount) {
		if(!allSwitchInfoList.containsKey(swid)) {
			System.out.println("ADD LAST COUNT, NO SW INFO FOR " + swid);
			return;
		}
		allSwitchInfoList.get(swid).setLastFlowCount(lastCount);
	}
	
	public void addCurrCount(long swid, long currCount) {
		if(!allSwitchInfoList.containsKey(swid)) {
			System.out.println("ADD CURR COUNT, NO SW INFO FOR " + swid);
			return;
		}
		allSwitchInfoList.get(swid).setCurrFlowCount(currCount);
	}
	
	public void setEdge(long swid, boolean isEdge) {
		if(!allSwitchInfoList.containsKey(swid)) {
			System.out.println("SET EDGE COUNT, NO SW INFO FOR " + swid);
			return;
		}
		allSwitchInfoList.get(swid).setEdge(isEdge);
	}
	
	public void constructEquationInfo() {
		for(String conid : conSwitchMap.keySet()) {
			EquationInfo info = new EquationInfo();

			info.nonhandler = conNonEventHandler.get(conid);
			ArrayList<Long> swlist = conSwitchMap.get(conid);
			info.numSwitch = swlist.size();
			for(Long swid : swlist) {
				
				if(!allSwitchInfoList.containsKey(swid)) {
					System.out.println("NO MEASUREMENT INFO FOR:" + swid);
					continue;
				}
				SwitchMeasurementInfo swinfo = allSwitchInfoList.get(swid);
				if(swinfo == null) {
					System.out.println("null swinfo:" + allSwitchInfoList.keySet());
				}
				
				if(swinfo.isEdge()) {
					info.newEdgeFlow += swinfo.getCurrFlowCount();
					info.oldEdgeFlow += swinfo.getLastFlowCount();
				} else {
					info.newCoreFlow += swinfo.getCurrFlowCount();
					info.oldCoreFlow += swinfo.getLastFlowCount();
				}
			}	

			boolean goodInfo = true;
			if(info.newEdgeFlow == 0 && info.newCoreFlow == 0 && info.oldCoreFlow == 0 && info.oldEdgeFlow == 0) {
				goodInfo = false;
			}
			if(info.nonhandler == 0) {
				goodInfo = false;
			}
			if(goodInfo == false) {
				return;
			}
			
			if(controllerEquationInfo.containsKey(conid)) {
				LinkedList<EquationInfo> tempinfos = controllerEquationInfo.get(conid);
				for(EquationInfo tempinfo : tempinfos) {
					if(tempinfo.equal(info)) {
						goodInfo = false;
						break;
					}
				}
			}
			if(goodInfo == false) {
				return;
			}
			LinkedList<EquationInfo> infos;
			if(controllerEquationInfo.containsKey(conid)) {
				infos = controllerEquationInfo.get(conid);
				System.out.println("construct equation!" + (infos.size() + 1));
				if(infos.size() >= NUM_EQUATIONS) {
					infos.removeFirst();
				}				
				infos.addLast(info);
				if(infos.size() == NUM_EQUATIONS) {
					System.out.println("conid:" + conid + " now ready to compute!");
					for(EquationInfo tinfo : infos) {
						System.out.println(tinfo.toString());
					}
				}
			} else {
				infos = new LinkedList<SwitchMeasurementManager.EquationInfo>();
				infos.add(info);
				controllerEquationInfo.put(conid, infos);
			}
		}
	}
	
	public String displayInfo() {
		String s = "";
		for(String conid : conSwitchMap.keySet()) {
			s += "controller non-event time:" + conNonEventHandler.get(conid) + "\n";
			s += "switches for this conid:" + conid + "\n";
			for(long swid : conSwitchMap.get(conid)) {
				s += " " + swid;
			}
			s += "\n";
		}
		
		s += "==========>num of switches:" + allSwitchInfoList.size() + "\n";
		for(Long swid : allSwitchInfoList.keySet()) {
			s += "for switch:" + swid + ":";
			SwitchMeasurementInfo info = allSwitchInfoList.get(swid);
			s += " edge:" + info.isEdge;
			s += " currentF:" + info.getCurrFlowCount();
			s += " lastF:" + info.getLastFlowCount();
			s += " eventFraction:" + info.getEventFraction();
			s += "\n";
		}
		return s;
	}
}
