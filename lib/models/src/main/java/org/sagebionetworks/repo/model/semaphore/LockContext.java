package org.sagebionetworks.repo.model.semaphore;

import java.util.Objects;
import java.util.StringJoiner;

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
		TableStatusDelete("Deleting status for table: '%s' ..."),
		TableSnapshot("Creating a snapshot of table: '%s' ..."),
		ViewSnapshot("Creating a snapshot of view: '%s' ..."),
		BuildTableIndex("Rebuilding table index: '%s' ..."),
		BuildViewIndex("Rebuilding view inxex: '%s' ..."),
		UpdatingViewIndex("Updating view inxex: '%s' ..."),
		Query("Querying table/view: '%s' ..."),
		BuildMaterializedView("Rebuilding materialized view: '%s' ..."),
		UpdatingMaterializedView("Updating materialized view: '%s' ...");

		String template;

		private ContextType(String template) {
			this.template = template;
		}

		public String getDisplayStringTemplate() {
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
		ValidateArgument.required(type, "type");
		ValidateArgument.required(objectId, "objectId");
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
		StringJoiner joiner = new StringJoiner(",");
		joiner.add(type.name());
		return joiner.add(objectId.toString()).toString();
	}

	/**
	 * Create a string that can be displayed to a user or logged.
	 * 
	 * @return
	 */
	public String toDisplayString() {
		return String.format(this.type.getDisplayStringTemplate(), this.objectId.toString());
	}
	
	/**
	 * Generate a message indicating that this context is waiting on the passed
	 * context.
	 * 
	 * @param waitingOn
	 * @return
	 */
	public String toWaitingOnMessage(LockContext waitingOn) {
		ValidateArgument.required(waitingOn, "waitingOn");
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
