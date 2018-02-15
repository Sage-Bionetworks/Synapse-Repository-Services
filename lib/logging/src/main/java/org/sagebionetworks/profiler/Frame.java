package org.sagebionetworks.profiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.util.IntervalStatistics;
import org.sagebionetworks.util.ValidateArgument;

public class Frame {
	private String name;
	private IntervalStatistics elapsedTimeStatistics;
	private LinkedHashMap<String,Frame> children;

	private static final String INDENT_STRING = "----";

	public Frame(String name) {
		this.name = name;
		this.elapsedTimeStatistics = new IntervalStatistics();
		this.children = new LinkedHashMap<>();
	}

	public Frame addChildFrameIfAbsent(String methodName){
		Frame childFrame = children.get(methodName);
		if (childFrame == null){
			childFrame = new Frame(methodName);
			children.put(methodName, childFrame);
		}
		return childFrame;
	}

	public String toString() {
		// Output a single line if there are no children.
		long id = Thread.currentThread().getId();
		StringBuilder buffer = new StringBuilder();
		buffer.append(String.format(
				"%n[%1$d] ELAPSE: Total: ", id));
		appendStatisticsToStringBuilder(buffer, this);
		if (children != null) {
			printChildren(buffer, id, 1);
		}
		return buffer.toString();
	}

	public void printChildren(StringBuilder buffer, long id, int level) {
		String indentString = StringUtils.repeat(INDENT_STRING, level);
		for (Frame child: children.values()) {
			buffer.append(String.format("%n[%d] ELAPSE: %s", id, indentString));
			appendStatisticsToStringBuilder(buffer, child);
			child.printChildren(buffer, id, level + 1);
		}
	}

	private static void appendStatisticsToStringBuilder(StringBuilder buffer, Frame frame){
		buffer.append( String.format("%1$tH:%1$tM:%1$tS:%1$tL METHOD: %2$s", frame.getTotalTimeMilis(), frame.getName()));
		if (frame.getCallsCount() > 1){
			buffer.append(String.format(" Count: %1$d (Average: %2$tH:%2$tM:%2$tS:%2$tL, Min: %3$tH:%3$tM:%3$tS:%3$tL, Max: %4$tH:%4$tM:%4$tS:%4$tL )",
										frame.getCallsCount(), frame.getAverageTimeMilis(), frame.getMinTimeMilis(), frame.getMaxTimeMilis()));
		}
	}

	public void addChild(Frame child) {
		ValidateArgument.requirement(!children.containsKey(child.name), "Child with name " + child.name + " already exists.");
		children.put(child.name, child);
	}


	public String getName() {
		return name;
	}

	public Frame getChild(String methodName){
		return children.get(methodName);
	}

	public List<Frame> getChildren() { //TODO: don't need?
		return new ArrayList<>(children.values());
	}

	public void addElapsedTime(long elapsedMilliseconds){
		this.elapsedTimeStatistics.addValue(elapsedMilliseconds);
	}

	public long getTotalTimeMilis(){
		return Math.round(this.elapsedTimeStatistics.getValueSum());
	}

	public long getCallsCount(){
		return this.elapsedTimeStatistics.getValueCount();
	}

	public long getAverageTimeMilis(){
		return Math.round(this.elapsedTimeStatistics.getValueCount() == 0 ? 0 : this.elapsedTimeStatistics.getValueSum() / this.elapsedTimeStatistics.getValueCount());
	}

	public long getMinTimeMilis(){
		return Math.round(this.elapsedTimeStatistics.getMinimumValue());
	}

	public long getMaxTimeMilis(){
		return Math.round(this.elapsedTimeStatistics.getMaximumValue());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Frame frame = (Frame) o;
		return Objects.equals(name, frame.name) &&
				Objects.equals(elapsedTimeStatistics, frame.elapsedTimeStatistics) &&
				Objects.equals(children, frame.children);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, elapsedTimeStatistics, children);
	}
}