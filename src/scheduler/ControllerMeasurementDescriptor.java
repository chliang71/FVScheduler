package scheduler;

public class ControllerMeasurementDescriptor {
	
	//f(Flows) = A*#old_edge + B*#new_edge + C*#old_core + D*#new_core
	double factor_old_flows_edge;
	double factor_new_flows_edge;
	
	double factor_old_flows_core;
	double factor_new_flows_core;
	
	//f(Switches) = E*#switches
	double factor_switches;
	
	//f(Background) = F
	double factor_background;
		
	int numSwitches;
	
	int numOldEdge;
	int numNewEdge;
	int numOldCore;
	int numNewCore;
	
	
	
}
