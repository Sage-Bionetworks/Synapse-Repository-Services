package org.sagebionetworks.repo.model.semaphore;

import java.util.Objects;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionParser;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Captures the context of a lock acquisition as a serialized string that can be
 * passed to
 * {@link CountingSemaphore#attemptToAcquireLock(String, long, int, String)}
 * 
 *
 */
public class LockContext {

	public enum ContextType {
		TableUpdate("Applying an update to table: '%s' ..."),
		TableSnapshot("Creating a snapshot of table: '%s' ..."),
		ViewSnapshot("Creating a snapshot of view: '%s' ..."),
		BuildTableIndex("Rebuliding table index: '%s' ..."),
		BuildViewIndex("Rebuilding view inxex: '%s' ..."),
		UpdatingViewIndex("Updating view inxex: '%s' ..."),
		Query("Querying table/view: '%s' ..."),
		BuildMaterializedView("Rebuilding materialized view: '%s' ...");

		String template;

		private ContextType(String template) {
			this.template = template;
		}

		public String getDisplayStringTemplte() {
			return template;
		}
	}

	private final ContextType type;
	private final IdAndVersion objectId;
	
	/**
	 * 
	 * @param type
	 * @param objectId
	 */
	public LockContext(ContextType type, IdAndVersion objectId) {
		this.type = type;
		this.objectId = objectId;
	}


	/**
	 * Create a new lock context from its serialized string.
	 * 
	 * @param serialized
	 */
	public static LockContext deserialize(String serialized) {
		ValidateArgument.required(serialized, "Serialized string cannot be null");
		String[] parts = serialized.split(",");
		if (parts.length != 2) {
			throw new IllegalArgumentException(String.format("Invalid serialized string: '%s'", serialized));
		}
		return new LockContext(ContextType.valueOf(parts[0]), IdAndVersionParser.parseIdAndVersion(parts[1]));
	}

	/**
	 * Serialize this lock context to a string that can be provided when the lock is
	 * acquired.
	 * 
	 * @return
	 */
	public String serializeToString() {
		StringBuilder builder = new StringBuilder();
		serializeToString(builder);
		return builder.toString();
	}

	void serializeToString(StringBuilder builder) {
		builder.append(type.name());
		builder.append(",");
		builder.append(objectId.toString());
	}

	/**
	 * Create a string that can be displayed to a user or logged.
	 * 
	 * @return
	 */
	public String toDisplayString() {
		return String.format(this.type.getDisplayStringTemplte(), this.objectId.toString());
	}
	
	public String toLogString() {
		return String.format("[%s, %s]", this.type.name(), this.objectId.toString());
	}
	
	/**
	 * Generate a message indicating that this context is waiting on the passed
	 * context.
	 * 
	 * @param waitingOn
	 * @return
	 */
	public String toWaitingOnMessage(LockContext waitingOn) {
		return String.format("[%s, %s] waiting on [%s, %s]", this.type.name(), this.objectId.toString(),
				waitingOn.type.name(), waitingOn.objectId.toString());
	}


	@Override
	public int hashCode() {
		return Objects.hash(objectId, type);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LockContext other = (LockContext) obj;
		return Objects.equals(objectId, other.objectId) && type == other.type;
	}


	@Override
	public String toString() {
		return "LockContext [type=" + type + ", objectId=" + objectId + "]";
	}

}
