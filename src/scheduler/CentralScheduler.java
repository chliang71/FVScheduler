package scheduler;

import randy.model.ModelInput;

public class CentralScheduler {
	
	InformationCollector ic;
	Thread collectorThread;
	
	public CentralScheduler() {
		ic = new InformationCollector();
	}
	
	public void schedule() {
		ic.startCollecting();
		
		while(true) {
			try {
				int[][] consumption = ic.retriveMatrix();
				ModelInput input = new ModelInput();
				input.setControllerNumber(ic.getNumControllre());
				input.setSwitchNumber(ic.getNumSwitch());
				input.setConsumpiton(consumption);
				//solve
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
