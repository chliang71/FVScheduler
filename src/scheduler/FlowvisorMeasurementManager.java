package scheduler;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.flowvisor.measurement.MeasurementInfo;
import org.flowvisor.measurement.MeasurementRMI;

public class FlowvisorMeasurementManager {
	MeasurementRMI flowvisorRMI;
	
	public boolean initRMI(String id, String host, int port) {
		Registry registry;
		boolean succeed = true;
		try {
			registry = LocateRegistry.getRegistry(host, port);
			flowvisorRMI = (MeasurementRMI) registry.lookup(id);
		} catch (RemoteException e) {
			e.printStackTrace();
			succeed = false;
		} catch (NotBoundException e) {
			e.printStackTrace();
			succeed = false;
		}
		return succeed;
	}
	
	public MeasurementInfo getFlowvisorInfo() {
		MeasurementInfo info = null;
		try {
			info = flowvisorRMI.getCurrentMeasurementInfoAndRefresh();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return info;
	}
}
