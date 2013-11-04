package scheduler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SliceStats {

	String sliceName;
	SliceStatCategory rx;
	SliceStatCategory tx;
	SliceStatCategory drop;
	
	public void aggreate(SliceStats ss) {
		if (!this.sliceName.equals(ss.sliceName)) {
			System.out.println("ERROR: aggreating different slice!");
			return;
		}
		this.rx.aggreate(ss.rx);
		this.tx.aggreate(ss.tx);
		this.drop.aggreate(ss.drop);
	}
	
	public void parseFromJson(String jsonString) {
		JsonElement jelement = new JsonParser().parse(jsonString);
		JsonObject jobject = jelement.getAsJsonObject();
		
		JsonObject subjobject = jobject.getAsJsonObject("rx");
		rx = parseHelper(subjobject);
		System.out.println("rx-----------\n" + rx);
		
		subjobject = jobject.getAsJsonObject("tx");
		tx = parseHelper(subjobject);
		System.out.println("tx------------\n" + tx);
		
		subjobject = jobject.getAsJsonObject("drop");
		drop = parseHelper(subjobject);
		System.out.println("drop------------\n" + drop);
		
	}
	
	private SliceStatCategory parseHelper(JsonObject jobject){
		jobject = jobject.getAsJsonObject("Total");
		if (jobject.has("STATS_REQUEST.DESC")) {
			jobject.addProperty("STATS_REQUEST_DESC", jobject.
					get("STATS_REQUEST.DESC").getAsString());
		}
		Gson gson = new Gson();
		return gson.fromJson(jobject, SliceStatCategory.class);
	}
	
	class SliceStatCategory {
		int BARRIER_REPLY; 
		int BARRIER_REQUEST; 
		int ECHO_REPLY; 
		int ECHO_REQUEST; 
		int ERROR;
		int FEATURES_REPLY; 
		int FEATURES_REQUEST; 
		int FLOW_MOD;
		int GET_CONFIG_REPLY; 
		int GET_CONFIG_REQUEST; 
		int HELLO;
		int PACKET_IN; 
		int SET_CONFIG;
		int STATS_REPLY_DESC; 
		int STATS_REQUEST_DESC;
		int PORT_STATUS;
		
		public void aggreate(SliceStatCategory ssc) {
			this.BARRIER_REPLY += ssc.BARRIER_REPLY;
			this.BARRIER_REQUEST += ssc.BARRIER_REQUEST;
			this.ECHO_REPLY += ssc.ECHO_REPLY;
			this.ECHO_REQUEST += ssc.ECHO_REQUEST;
			this.ERROR += ssc.ERROR;
			this.FEATURES_REPLY += ssc.FEATURES_REPLY;
			this.FEATURES_REQUEST += ssc.FEATURES_REQUEST;
			this.FLOW_MOD += ssc.FLOW_MOD;
			this.GET_CONFIG_REPLY += ssc.GET_CONFIG_REPLY;
			this.GET_CONFIG_REQUEST += ssc.GET_CONFIG_REQUEST;
			this.HELLO += ssc.HELLO;
			this.PACKET_IN += ssc.PACKET_IN;
			this.PORT_STATUS += ssc.PORT_STATUS;
			this.SET_CONFIG += ssc.SET_CONFIG;
			this.STATS_REPLY_DESC += ssc.STATS_REPLY_DESC;
			this.STATS_REQUEST_DESC += ssc.STATS_REQUEST_DESC;
		}
		
		@Override
		public String toString() {
			return this.BARRIER_REPLY +
					"::" + this.BARRIER_REQUEST +
					"::" + this.ECHO_REPLY +
					"::" + this.ECHO_REQUEST +
					"::" + this.ERROR + 
					"::" + this.FEATURES_REPLY +
					"::" + this.FEATURES_REQUEST +
					"::" + this.FLOW_MOD +
					"::" + this.GET_CONFIG_REPLY +
					"::" + this.GET_CONFIG_REQUEST +
					"::" + this.HELLO +
					"::" + this.PACKET_IN +
					"::" + this.SET_CONFIG +
					"::" + this.STATS_REPLY_DESC +
					"::" + this.STATS_REQUEST_DESC +
					"::" + this.PORT_STATUS;
		}
	}
}

