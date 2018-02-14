package org.sagebionetworks.profiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.util.IntervalStatistics;
import org.sagebionetworks.util.ValidateArgument;

public class Frame {

	/**
	 * JSON name constants
	 */
	private static final String NAME 		= "name";
	private static final String CHILDREN 	= "children";

	private String name;
	private IntervalStatistics elapsedTimeStatistics;
	private LinkedHashMap<String,Frame> children;

	public Frame() {
	}

	public Frame(String name) {
		this.name = name;
		this.elapsedTimeStatistics = new IntervalStatistics();
		this.children = new LinkedHashMap<>();
	}

	public Frame addFrameIfAbsent(String methodName){
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
				"%n[%3$d] ELAPSE: Total: %1$tH:%1$tM:%1$tS:%1$tL METHOD: %2$s", elapsedTimeStatistics, //TODO: figure out print
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
		for (Frame child: children.values()) {
			buffer.append(String
					.format("%n[%5$d] ELAPSE: %2$s%1$tM:%1$tS:%1$tL METHOD: %3$s",
							child.elapsedTimeStatistics, level, child.name, time, id));
			child.printChildren(buffer, id, level + "----", time);
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

	public IntervalStatistics getElapsedTimeStatistics() {
		return elapsedTimeStatistics;
	}

	public List<Frame> getChildren() { //TODO: don't need?
		return new ArrayList<>(children.values());
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addElapsedTime(long elapsedMilliseconds){ //TODO conversion of long to double?
		this.elapsedTimeStatistics.addValue(elapsedMilliseconds);
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

	/**
	 * Write a given frame to JSON
	 *
	 * @param frame
	 * @return
	 * @throws JSONException
	 */
	public static String writeFrameJSON(Frame frame) throws JSONException {
		// Create root
		JSONObject root = writeToJSONObject(frame);
		return root.toString();
	}

	//TODO:fix json stuff
	/**
	 * Recursive method to create the JSON objects for a tree of frames.
	 *
	 * @param frame
	 * @return
	 * @throws JSONException
	 */
	public static JSONObject writeToJSONObject(Frame frame)
			throws JSONException {
		JSONObject object = new JSONObject();
		object.put(NAME, frame.getName());
		if (frame.getChildren() != null) {
			for (Frame child : frame.getChildren()) {
				object.accumulate(CHILDREN, writeToJSONObject(child));
			}
		}
		return object;
	}

	/**
	 * Read a Frame from a JSON String
	 *
	 * @param jsonString
	 * @return
	 * @throws JSONException
	 */
	public static Frame readFrameFromJSON(String jsonString)
			throws JSONException {
		JSONObject object = new JSONObject(jsonString);
		return writeToJSONObject(object);
	}

	/**
	 * Recursive method to build up a Frame from a JSON object.
	 *
	 * @param object
	 * @return
	 * @throws JSONException
	 */
	public static Frame writeToJSONObject(JSONObject object)
			throws JSONException {
		Frame frame = new Frame();
		frame.setName(object.getString(NAME));
		if (object.has(CHILDREN)) {
			JSONArray children = object.getJSONArray(CHILDREN);
			for (int i = 0; i < children.length(); i++) {
				JSONObject child = children.getJSONObject(i);
				Frame childFrame = writeToJSONObject(child);
				frame.addChild(childFrame);
			}
		}
		return frame;
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