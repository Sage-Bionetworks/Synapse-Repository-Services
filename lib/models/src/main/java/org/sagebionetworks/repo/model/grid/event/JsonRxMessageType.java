package org.sagebionetworks.repo.model.grid.event;

/**
 * See: <a href="https://jsonjoy.com/specs/json-rx/json-encoding">JSON-Rx
 * Encoding</a>
 */
public enum JsonRxMessageType {

	RequestData(1),
	RequestError(2),
	RequestUnsubscribe(3),
	ResponseData(4),
	ResponseComplete(5),
	ResponseError(6),
	ResposneUnsubscribe(7),
	Notification(8);

	final int code;

	private JsonRxMessageType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static JsonRxMessageType fromCode(int code) {
		for(JsonRxMessageType t: JsonRxMessageType.values()) {
			if(t.code == code) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown JSON-Rx code: "+code);
	}
}
