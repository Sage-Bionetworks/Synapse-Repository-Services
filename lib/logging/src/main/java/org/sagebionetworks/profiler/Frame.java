package org.sagebionetworks.profiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.sagebionetworks.util.IntervalStatistics;

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
		addFramesToBuffer(Collections.singletonList(this), buffer, id, 0);
		return buffer.toString();
	}

	static void addFramesToBuffer(Collection<Frame> toPrint, StringBuilder buffer, long id, int level) {
		String indentString = StringUtils.repeat(INDENT_STRING, level);
		for (Frame child: toPrint) {
			buffer.append(String.format("%n[%d] ELAPSE:%s", id, indentString));
			appendStatisticsToStringBuilder(buffer, child);
			addFramesToBuffer(child.children.values(), buffer, id, level + 1);
		}
	}

	private static void appendStatisticsToStringBuilder(StringBuilder buffer, Frame frame){
		buffer.append( String.format("%s METHOD: %s", formatDuration(frame.getTotalTimeMilis()), frame.getName()));
		if (frame.getCallsCount() > 1){
			buffer.append(String.format(" (Count: %d Average: %s, Min: %s, Max: %s)",
										frame.getCallsCount(),
										formatDuration(frame.getAverageTimeMilis()),
										formatDuration(frame.getMinTimeMilis()),
										formatDuration(frame.getMaxTimeMilis())));
		}
	}

	static String formatDuration(long milliseconds){
		return DurationFormatUtils.formatDuration(milliseconds, "HH:mm:ss.S");
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