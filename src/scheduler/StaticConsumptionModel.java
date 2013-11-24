package scheduler;

import java.util.HashMap;

public class StaticConsumptionModel implements ConsumptionModel {
	
	HashMap<MessageType, Integer> consumption;
	
	public StaticConsumptionModel() {
		consumption = new HashMap<MessageType, Integer>();
		consumption.put(MessageType.BARRIER_REPLY, 1);
		consumption.put(MessageType.BARRIER_REQUEST, 1);
		consumption.put(MessageType.ECHO_REPLY, 1);
		consumption.put(MessageType.ECHO_REQUEST, 1);
		consumption.put(MessageType.ERROR, 1);
		consumption.put(MessageType.FEATURES_REPLY, 1);
		consumption.put(MessageType.FEATURES_REQUEST, 1);
		consumption.put(MessageType.FLOW_MOD, 5);
		consumption.put(MessageType.GET_CONFIG_REPLY, 1);
		consumption.put(MessageType.GET_CONFIG_REQUEST, 1);
		consumption.put(MessageType.HELLO, 1);
		consumption.put(MessageType.PACKET_IN, 5);
		consumption.put(MessageType.PORT_STATUS, 1);
		consumption.put(MessageType.SET_CONFIG, 1);
		consumption.put(MessageType.STATS_REPLY_DESC, 1);
		consumption.put(MessageType.STATS_REQUEST_DESC, 1);
		consumption.put(MessageType.PORT_MOD, 1);
		consumption.put(MessageType.FLOW_REMOVED, 1);
		consumption.put(MessageType.QUEUE_CONFIG_REPLY, 1);
		consumption.put(MessageType.QUEUE_CONFIG_REQUEST, 1);
		consumption.put(MessageType.PACKET_OUT, 5);
	}
	
	@Override
	public int generateConsumption(MessageCounts2 mc) {
		/*int cur = 0;
		for(MessageType type : MessageType.values()) {
			cur += mc.getCount(type)*consumption.get(type);
		}
		return cur;*/
		int cur = 0;
		cur += mc.BARRIER_REPLY* consumption.get(MessageType.BARRIER_REPLY);
		cur += mc.BARRIER_REQUEST* consumption.get(MessageType.BARRIER_REQUEST);
		cur += mc.ECHO_REPLY* consumption.get(MessageType.ECHO_REPLY);
		cur += mc.ECHO_REQUEST* consumption.get(MessageType.ECHO_REQUEST);
		cur += mc.ERROR* consumption.get(MessageType.ERROR);
		cur += mc.FEATURES_REPLY* consumption.get(MessageType.FEATURES_REPLY);
		cur += mc.FEATURES_REQUEST* consumption.get(MessageType.FEATURES_REQUEST);
		cur += mc.FLOW_MOD* consumption.get(MessageType.FLOW_MOD);
		cur += mc.FLOW_REMOVED* consumption.get(MessageType.FLOW_REMOVED);
		cur += mc.GET_CONFIG_REPLY* consumption.get(MessageType.GET_CONFIG_REPLY);
		cur += mc.GET_CONFIG_REQUEST* consumption.get(MessageType.GET_CONFIG_REQUEST);
		cur += mc.HELLO* consumption.get(MessageType.HELLO);
		cur += mc.PACKET_IN* consumption.get(MessageType.PACKET_IN);
		cur += mc.PACKET_OUT* consumption.get(MessageType.PACKET_OUT);
		cur += mc.PORT_MOD* consumption.get(MessageType.PORT_MOD);
		cur += mc.PORT_STATUS* consumption.get(MessageType.PORT_STATUS);
		cur += mc.QUEUE_CONFIG_REPLY* consumption.get(MessageType.QUEUE_CONFIG_REPLY);
		cur += mc.QUEUE_CONFIG_REQUEST* consumption.get(MessageType.QUEUE_CONFIG_REQUEST);
		cur += mc.SET_CONFIG* consumption.get(MessageType.SET_CONFIG);
		cur += mc.STATS_REPLY_DESC* consumption.get(MessageType.STATS_REPLY_DESC);
		cur += mc.STATS_REQUEST_DESC* consumption.get(MessageType.STATS_REQUEST_DESC);
		
		return cur;
	}
}
