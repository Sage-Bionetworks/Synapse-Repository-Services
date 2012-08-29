package org.sagebionetworks.logging;

import java.util.Map;

import org.joda.time.DateTime;

public class SynapseEvent {


	private DateTime timeStamp;
	private String controller, methodName;

	private int latency;
	private Map<String, String> properties;

	public SynapseEvent(DateTime timeStamp, String controller,
			String methodName, int latency, Map<String, String> properties) {
		this.timeStamp = timeStamp;
		this.controller = controller;
		this.methodName = methodName;
		this.latency = latency;
		this.properties = properties;
	}

	public DateTime getTimeStamp() {
		return timeStamp;
	}

	public String getController() {
		return controller;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getLatency() {
		return latency;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

}
