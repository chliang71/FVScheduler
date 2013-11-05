package scheduler;

import scheduler.SliceStats.SliceStatCategory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SwitchStats {
	//not sure whether we will actually need this...
	//because this will be helpful only when we 
	//are interested in re-balancing load based 
	//on the load of each switch, but there is
	//not such thing in the model so far
	SliceStatCategory rx;
	SliceStatCategory tx;
	SliceStatCategory drop;
	
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
	
	public void parseFromCmd(String string) {
		JsonElement jelement = new JsonParser().parse(string);
		JsonObject jobject = jelement.getAsJsonObject();
		
		JsonObject subjobject = jobject.getAsJsonObject("rx");
		rx = parseHelper(subjobject);
		//System.out.println("rx-----------\n" + rx);
		
		subjobject = jobject.getAsJsonObject("tx");
		tx = parseHelper(subjobject);
		//System.out.println("tx------------\n" + tx);
		
		subjobject = jobject.getAsJsonObject("drop");
		drop = parseHelper(subjobject);
		//System.out.println("drop------------\n" + drop);
				
	}
	
	private SliceStatCategory parseHelper(JsonObject jobject){
		jobject = jobject.getAsJsonObject("Total");
		if (jobject.has("STATS_REQUEST.DESC")) {
			jobject.addProperty("STATS_REQUEST_DESC", 
					jobject.get("STATS_REQUEST.DESC").getAsString());
		}
		if (jobject.has("STATS_REPLY.DESC")) {
			jobject.addProperty("STATS_REPLY_DESC", 
					jobject.get("STATS_REPLY.DESC").getAsString());
		}
		Gson gson = new Gson();
		return gson.fromJson(jobject, SliceStatCategory.class);
	}
	
	class SwitchStatCategory {
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
