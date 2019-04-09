package org.sagebionetworks.profiler;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.sagebionetworks.util.IntervalStatistics;

public class Frame {
	private String name;
	private IntervalStatistics elapsedTimeStatistics;
	private LinkedHashMap<String,Frame> children;

	static final String INDENT_STRING = "----";

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
		StringBuilder buffer = new StringBuilder();
		addFramesToBuffer(Collections.singletonList(this), buffer, 0);
		return buffer.toString();
	}

	static void addFramesToBuffer(Collection<Frame> toPrint, StringBuilder buffer, int level) {
		/* NOTE: We chose a recursive implementation instead of an iterative one because it is much cleaner.
		 Additionally, in our use case for this class, there will never be a risk of StackOverflowError. This is because the n-ary tree that a
		 Frame represents is built up by the Profiler, which monitors the execution of methods. The tree of Frames' height (i.e. max number of recursive calls) is therefore
		 at most the number of the call stack of monitored methods. If call stacks were to overflow, it would occur during the execution  of the monitored methods, before a complete
		 Frame tree can be built. By the time we call addFramesToBuffer(), a complete tree of Frames has been built, so we are guaranteed that the number of recursive calls will not cause StackOverflowError
		*/
		String indentString = StringUtils.repeat(INDENT_STRING, level);
		for (Frame frame : toPrint) {
			frame.appendStatisticsToStringBuilder(buffer, indentString);
			addFramesToBuffer(frame.children.values(), buffer, level + 1);
		}
	}

	void appendStatisticsToStringBuilder(StringBuilder buffer, String indentString){
		buffer.append(String.format("%n[%d] ELAPSE:%s", Thread.currentThread().getId(), indentString));
		buffer.append( String.format("%s METHOD: %s", formatDuration(this.getTotalTimeMilis()), this.getName()));
		if (this.getCallsCount() > 1){
			buffer.append(String.format(" (Count: %d Average: %s, Min: %s, Max: %s)",
										this.getCallsCount(),
										formatDuration(this.getAverageTimeMilis()),
										formatDuration(this.getMinTimeMilis()),
										formatDuration(this.getMaxTimeMilis())));
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