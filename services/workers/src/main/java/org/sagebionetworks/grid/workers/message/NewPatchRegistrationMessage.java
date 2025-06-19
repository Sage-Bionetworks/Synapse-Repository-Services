package org.sagebionetworks.grid.workers.message;

import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;

/**
 * Message from a replica to the hub to register a new patch.
 */
public class NewPatchRegistrationMessage implements RequestDataMessage {

	private final LogicalTimestamp patchId;
	private final Integer requestId;
	private final EventContext context;
	private final String body;

	public NewPatchRegistrationMessage(EventContext context, Integer id, JSONArray body) {
		this.patchId = PatchCompactSerializable.peekPatchId(body);
		this.requestId = id;
		this.context = context;
		this.body = body.toString();
	}

	/**
	 * The ID of the patch.
	 * 
	 * @return
	 */
	public LogicalTimestamp getPatchId() {
		return patchId;
	}

	/**
	 * Request ID provided by the client making this request.
	 * 
	 * @return
	 */
	public Integer getRequestId() {
		return requestId;
	}

	public EventContext getContext() {
		return context;
	}

	public String getBody() {
		return body;
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, context, patchId, requestId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewPatchRegistrationMessage other = (NewPatchRegistrationMessage) obj;
		return Objects.equals(body, other.body) && Objects.equals(context, other.context)
				&& Objects.equals(patchId, other.patchId) && Objects.equals(requestId, other.requestId);
	}

	@Override
	public String toString() {
		return "PatchDataRequest [patchId=" + patchId + ", requestId=" + requestId + ", context=" + context + ", body="
				+ body + "]";
	}

}
