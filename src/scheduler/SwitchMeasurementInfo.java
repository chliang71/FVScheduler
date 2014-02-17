package scheduler;

public class SwitchMeasurementInfo {
	
	Long swid;
	
	Boolean isEdge;
	
	Long lastFlowCount;
	Long currFlowCount;
	
	Double eventFraction;
	
	public SwitchMeasurementInfo() {
		this.swid = null;
		this.isEdge = false;
		this.lastFlowCount = null;
		this.currFlowCount = null;
		this.eventFraction = null;
	}
	
	public void setEventFraction(double fraction) {
		this.eventFraction = fraction;
	}
	
	public Double getEventFraction() {
		return this.eventFraction;
	}
	
	public void setSwid(long swid) {
		this.swid = swid;
	}
	
	public Long getSwid() {
		return this.swid;
	}
	
	public boolean isEdge() {
		return this.isEdge;
	}
	
	public void setEdge(boolean isEdge) {
		this.isEdge = isEdge;
	}
	
	public Long getLastFlowCount() {
		return this.lastFlowCount;
	}
	
	public void setLastFlowCount(long count) {
		this.lastFlowCount = new Long(count);
	}
	
	public Long getCurrFlowCount() {
		return this.currFlowCount;
	}
	
	public void setCurrFlowCount(long count) {
		this.currFlowCount = new Long(count);
	}
}
