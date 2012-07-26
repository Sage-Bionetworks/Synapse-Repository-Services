package profiler.org.sagebionetworks.usagemetrics;

import java.lang.reflect.Method;
import java.util.Date;

public class LogData {
	long latency;
	Class controller;
	String serviceCall;
	String args;

	public long getLatency() {
		return latency;
	}

	public void setLatency(long latency) {
		this.latency = latency;
	}

	public Class getController() {
		return controller;
	}

	public void setController(Class controller) {
		this.controller = controller;
	}

	public String getServiceCall() {
		return serviceCall;
	}

	public void setServiceCall(String serviceCall) {
		this.serviceCall = serviceCall;
	}

	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(latency);
		sb.append("ms ");
		sb.append(controller.getSimpleName());
		sb.append(".");
		sb.append(serviceCall);
		sb.append("(");
		sb.append(args);
		sb.append(")");
		return latency + "ms " + controller.getSimpleName() + "." + serviceCall + "(" + args + ")";
	}
	
}
