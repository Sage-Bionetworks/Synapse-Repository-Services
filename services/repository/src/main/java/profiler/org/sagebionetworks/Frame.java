package profiler.org.sagebionetworks;

import java.util.ArrayList;
import java.util.List;

class Frame {

	private long startTime = -1;
	private String name = null;
	private long elapse = -1;
	private List<Frame> children = null;

	public Frame(long startTime, String name) {
		this.startTime = startTime;
		this.name = name;
	}

	public String toString() {
		// Output a single line if there are no children.
		long id = Thread.currentThread().getId();
		StringBuilder buffer = new StringBuilder();
		buffer.append(String.format(
				"%n[%3$d] ELAPSE: %1$tM:%1$tS:%1$tL METHOD: %2$s", elapse,
				name, id));
		if (children == null) {
			return buffer.toString();
		} else {
			printChildren(buffer, id, "----", System.currentTimeMillis());
			// Now print the children
			return buffer.toString();
		}
	}

	public void printChildren(StringBuilder buffer, long id, String level,
			long time) {
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				Frame child = children.get(i);
				buffer.append(String
						.format("%n[%5$d] ELAPSE: %2$s%1$tM:%1$tS:%1$tL METHOD: %3$s",
								child.elapse, level, child.name, time, id));
				if (child.children != null) {
					child.printChildren(buffer, id, level + "----", time);
				}
			}
		}
	}

	public void setEnd(long end) {
		this.elapse = (end - startTime)/1000000;
	}

	public void addChild(Frame child) {
		if (children == null) {
			children = new ArrayList<Frame>();
		}
		children.add(child);
	}
}