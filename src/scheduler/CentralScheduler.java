package scheduler;

import randy.model.ModelInput;

public class CentralScheduler {
	
	InformationCollector ic;
	Thread collectorThread;
	
	public static void main(String[] args) {
		CentralScheduler cs = new CentralScheduler();
		cs.schedule();
	}
	
	public CentralScheduler() {
		ic = new InformationCollector();
	}
	
	public void schedule() {
		ic.startCollecting();
		
		while(true) {
			try {
				int[][] consumption = ic.retriveMatrix();
				if (consumption != null) {
					System.out.println("consumption received!");
					for(int i = 0;i<consumption.length;i++) {
						for(int j = 0;j<consumption[0].length;j++)
							System.out.print(consumption[i][j] + " ");
						System.out.println();
					}
					ModelInput input = new ModelInput();
					input.setControllerNumber(ic.getNumControllre());
					input.setSwitchNumber(ic.getNumSwitch());
					input.setConsumpiton(consumption);
					//solve
					int[] assignment = new int[consumption.length];
					for(int i = 0;i<assignment.length;i++) {
						assignment[i] = i%2;
					}
					ic.buildUpdate(assignment);
				} else {
					System.out.println("no input sleep");
				}
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
