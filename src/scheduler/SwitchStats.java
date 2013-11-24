package scheduler;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class SwitchStats {
	//not sure whether we will actually need this...
	//because this will be helpful only when we 
	//are interested in re-balancing load based 
	//on the load of each switch, but there is
	//not such thing in the model so far
	MessageCounts2 rx;
	MessageCounts2 tx;
	MessageCounts2 drop;
	MessageCounts2 aggregate;

	public void parseFromCmd(String string) {
		try {
			JsonElement jelement = new JsonParser().parse(string);
			JsonObject jobject = jelement.getAsJsonObject();

			JsonObject subjobject = jobject.getAsJsonObject("rx");
			rx = parseHelper(subjobject);

			subjobject = jobject.getAsJsonObject("tx");
			tx = parseHelper(subjobject);

			subjobject = jobject.getAsJsonObject("drop");
			drop = parseHelper(subjobject);
			aggregate = new MessageCounts2();
			aggregate.aggreate(rx);
			aggregate.aggreate(tx);
		} catch (JsonSyntaxException e) {
			System.out.println("An EOF exception is encountered" + string);
			e.printStackTrace();
		}
	}

	public SwitchStats substract(SwitchStats ss) {
		SwitchStats ssinc = new SwitchStats();
		ssinc.drop = this.drop.substract(ss.drop);
		ssinc.rx = this.rx.substract(ss.rx);
		ssinc.tx = this.tx.substract(ss.tx);
		ssinc.aggregate = this.aggregate.substract(ss.aggregate);
		return ssinc;
	}

	public int generateConsumption(ConsumptionModel cm) {
		//generate consumption based on current aggregate stats
		return cm.generateConsumption(aggregate);
	}

	private MessageCounts2 parseHelper(JsonObject jobject){
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
		return gson.fromJson(jobject, MessageCounts2.class);
	}


	@Override
	public String toString() {
		return "aggregate:" + aggregate.toString();
	}

}
