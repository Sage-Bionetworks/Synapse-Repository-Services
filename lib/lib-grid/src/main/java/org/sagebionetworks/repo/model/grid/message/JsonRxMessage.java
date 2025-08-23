package org.sagebionetworks.repo.model.grid.message;

import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Each {@link JsonRxMessage} is serialized as a JSON array and is composed of
 * four parts:
 * <ul>
 * <li>type - Code that identifies the type of message.</li>
 * <li>id - Some operations require multiple messages to be exchanged. For such
 * cases the client will issue a unique identifier to associate the group of
 * messages.</li>
 * <li>method name - Identifies specific operations under a type grouping.</li>
 * <li>body - Included for messages with additional request or response
 * data.</li>
 * </ul>
 * The message type and method will determine which parts should be included.
 * 
 * @see <a href="https://jsonjoy.com/specs/json-rx/messages">json-rx
 *      messages</a>
 * 
 */
public class JsonRxMessage {

	private final JsonRxMessageType type;
	private Integer id;
	private String method;
	private Object body;

	public JsonRxMessage(JsonRxMessageType type) {
		ValidateArgument.required(type, "type");
		this.type = type;
	}

	public JsonRxMessage(String json) {
		this(new JSONArray(json));
	}

	public JsonRxMessage(JSONArray array) {
		int zero = array.optInt(0, -1);
		if (zero < 1) {
			throw new IllegalArgumentException("Expected the fist element of the array to be a message code.");
		}
		this.type = JsonRxMessageType.fromCode(zero);
		Object one = array.opt(1);
		Object two = array.opt(2);
		Object three = array.opt(3);
		this.method = one instanceof String ? (String) one : two instanceof String ? (String) two : null;
		this.id = one instanceof Number ? ((Number) one).intValue() : null;
		JSONObject bodyObject = two instanceof JSONObject ? (JSONObject) two
				: three instanceof JSONObject ? (JSONObject) three : null;
		JSONArray bodyArray = two instanceof JSONArray ? (JSONArray) two
				: three instanceof JSONArray ? (JSONArray) three : null;
		this.body = bodyObject != null ? bodyObject : bodyArray;
	}

	public JsonRxMessageType getType() {
		return type;
	}

	public Optional<Integer> getId() {
		return Optional.ofNullable(id);
	}

	public Optional<String> getMethod() {
		return Optional.ofNullable(method);
	}

	public Optional<Object> getBody() {
		return Optional.ofNullable(body);
	}

	public JsonRxMessage setId(Integer id) {
		this.id = id;
		return this;
	}

	public JsonRxMessage setMethod(String method) {
		this.method = method;
		return this;
	}

	public JsonRxMessage setBody(JSONArray body) {
		this.body = body;
		return this;
	}

	public JsonRxMessage setBody(JSONObject body) {
		this.body = body;
		return this;
	}

	String bodyAsString() {
		return body != null ? body.toString() : null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bodyAsString(), id, method, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonRxMessage other = (JsonRxMessage) obj;
		return Objects.equals(bodyAsString(), other.bodyAsString()) && Objects.equals(id, other.id)
				&& Objects.equals(method, other.method) && type == other.type;
	}

	public String toJson() {
		JSONArray array = new JSONArray();
		if (type != null) {
			array.put(type.code);
		}
		if (id != null) {
			array.put(id);
		}
		if (method != null) {
			array.put(method);
		}
		if (body != null) {
			array.put(body);
		}
		return array.toString();
	}

	@Override
	public String toString() {
		return toJson();
	}

}
