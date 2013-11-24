package scheduler;

public enum MessageType {
	BARRIER_REPLY, 
	BARRIER_REQUEST, 
	ECHO_REPLY, 
	ECHO_REQUEST, 
	ERROR,
	FEATURES_REPLY,
	FEATURES_REQUEST,
	FLOW_MOD,
	FLOW_REMOVED,
	GET_CONFIG_REPLY,
	GET_CONFIG_REQUEST, 
	HELLO,
	PACKET_IN,
	PACKET_OUT,
	PORT_MOD,
	SET_CONFIG,
	STATS_REPLY_DESC,
	STATS_REQUEST_DESC,
	PORT_STATUS,
	QUEUE_CONFIG_REQUEST,
	QUEUE_CONFIG_REPLY;
}