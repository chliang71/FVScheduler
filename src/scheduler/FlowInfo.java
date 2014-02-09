package scheduler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FlowInfo {
	
	String srcIP;
	String dstIP;
	
	String srcMac;
	String dstMac;
	
	public void parseFromJson(String jsonString) {
		JsonElement jelement = new JsonParser().parse(jsonString);
		JsonObject jobject = jelement.getAsJsonObject();
		
		//System.out.println(jelement + ":::" + jobject);
		JsonObject match = jobject.get("match").getAsJsonObject();
		//System.out.println("------->" + match);
		srcMac = match.get("dl_src").getAsString();
		dstMac = match.get("dl_dst").getAsString();
		//System.out.println("src mac:" + srcMac);
		//System.out.println("dst mac:" + dstMac);
		srcIP = match.get("nw_src").getAsString();
		dstIP = match.get("nw_dst").getAsString();
		//System.out.println("src ip:" + srcIP);
		//System.out.println("dst ip:" + dstIP);
	}
	
	@Override
	public int hashCode() {
		final int prime = 131;
		int result = 1;
		result = prime * result + this.dstIP.hashCode();
		result = prime * result + this.srcIP.hashCode();
		result = prime * result + this.dstMac.hashCode();
		result = prime * result + this.srcMac.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}		
		if(obj == null) {
			return false;
		}
		if(!(obj instanceof FlowInfo)) {
			return false;
		}
		FlowInfo info = (FlowInfo)obj;
		
		
		boolean ret = (info.dstIP.equals(this.dstIP)) && 
				(info.srcIP.equals(this.srcIP)) &&
				(info.dstMac.equals(this.dstMac)) && 
				info.srcMac.equals(this.srcMac);
		//System.out.println("comparing:" + this + "->" + info + " got:" + ret);
		return ret;
	}

	@Override
	public String toString() {
		String s = "srcip:" + this.srcIP + " dstip:" + this.dstIP
				+ " srcmac:" + this.srcMac + " dstmac:" + this.dstMac;
		return s;
	}
}
