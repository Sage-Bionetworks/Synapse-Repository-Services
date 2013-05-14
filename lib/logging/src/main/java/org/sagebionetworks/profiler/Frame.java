package org.sagebionetworks.profiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Frame {

	/**
	 * JSON name constants
	 */
	private static final String START_TIME 	= "startTime";
	private static final String ELAPSE 		= "elapse";
	private static final String NAME 		= "name";
	private static final String CHILDREN 	= "children";
	
	private long startTime = -1;
	private String name = null;
	private long elapse = -1;
	private List<Frame> children = null;

	public Frame() {
	}

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

	public long getStartTime() {
		return startTime;
	}

	public String getName() {
		return name;
	}

	public long getElapse() {
		return elapse;
	}

	public List<Frame> getChildren() {
		return children;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setElapse(long elapse) {
		this.elapse = elapse;
	}

	public void setChildren(List<Frame> children) {
		this.children = children;
	}
	
	/**
	 * Write a given frame to JSON
	 * 
	 * @param frame
	 * @return
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String writeFrameJSON(Frame frame) throws JSONException {
		// Create root
		JSONObject root = writeToJSONObject(frame);
		return root.toString();
	}

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
		;
		object.put(START_TIME, frame.getStartTime());
		object.put(NAME, frame.getName());
		object.put(ELAPSE, frame.getElapse());
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
		frame.setStartTime(object.getLong(START_TIME));
		frame.setElapse(object.getLong(ELAPSE));
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + (int) (elapse ^ (elapse >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (startTime ^ (startTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Frame other = (Frame) obj;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (elapse != other.elapse)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (startTime != other.startTime)
			return false;
		return true;
	}
	
}