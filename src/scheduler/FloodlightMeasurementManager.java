package scheduler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.measurement.MeasurementWorker;

public class FloodlightMeasurementManager {
	
	ConcurrentHashMap<String, MeasurementWorker> allRMIs;
	
	public boolean readRMIConfigFromFile(String filename) {
		
		boolean succed = true;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			do {
				line = br.readLine();
				if(line == null)
					break;
				System.out.println(line);
				String[] configs = line.trim().split(" ");
				String host = configs[0];
				String id = configs[1];
				int port = Integer.parseInt(configs[2]);
				System.out.println("host:" + host + " id:" + id + " port:" + port);
				Registry registry = LocateRegistry.getRegistry(host, port);
				MeasurementWorker server = 
						(MeasurementWorker) registry.lookup(id);
				allRMIs.put(id, server);
			} while(line != null);
			br.close();
			succed = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			succed = false;
		} catch (IOException e) {
			e.printStackTrace();
			succed = false;
		} catch (NotBoundException e) {
			e.printStackTrace();
			succed = false;
		} 
		return succed;
	}
	
	HashMap<Long, Double> currentSwitchCPUTimeFraction;
	
	public FloodlightMeasurementManager() {
		allRMIs = new ConcurrentHashMap<String, MeasurementWorker>();
		currentSwitchCPUTimeFraction = new HashMap<Long, Double>();
	}
	
	public void addNewRMI(String host, int port, String id) {
		try {
			Registry registry;
			registry = LocateRegistry.getRegistry(host, port);
			MeasurementWorker server = 
					(MeasurementWorker) registry.lookup(id);
			allRMIs.put(id, server);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}
	
	public double getSwitchTotalCpuTimeConsumption(Long swid) {
		return 0;
	}
	
	public HashMap<Long, Double> getAllRMIFraction() {
		currentSwitchCPUTimeFraction.clear();
		for(String id : allRMIs.keySet()) {
			MeasurementWorker server = allRMIs.get(id);
			try {
				HashMap<Long, Double> currentFraction= server.getAllFractionAndRefresh();
				if(currentFraction == null) {
					continue;
				}
				for(Long swid : currentFraction.keySet()) {
					if(currentSwitchCPUTimeFraction.containsKey(swid)) {
						System.out.println("NOTE one swid measured by different controllers?" + swid);
					}
					currentSwitchCPUTimeFraction.put(swid, currentFraction.get(swid));
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return currentSwitchCPUTimeFraction;
	}
}
