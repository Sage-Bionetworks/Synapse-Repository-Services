package org.sagebionetworks.repo.manager.grid.synch;

import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;

/**
 * The schema a source binds its rows to. A source that determines the session's
 * schema also determines its absence, so a {@code null} {@code schema$Id} means
 * the source binds no schema and the session must stop validating against the
 * one it last recorded.
 *
 * @param schema$Id the bound JSON schema $id, or null if the source binds none
 */
public record SessionSchema(String schema$Id) {

	/**
	 * @param binding the schema bound to a source, if any
	 * @return the schema a session synchronized with that source records
	 */
	public static SessionSchema of(Optional<JsonSchemaObjectBinding> binding) {
		return new SessionSchema(binding.map(b -> b.getJsonSchemaVersionInfo().get$id()).orElse(null));
	}

	/**
	 * Schemas are compared by $id, so a new version of a schema bound without a
	 * semantic version, whose $id is unchanged, is not a difference.
	 *
	 * @param session a grid session
	 * @return true if a synchronization would record a different schema on the
	 *         session than the one it holds
	 */
	public boolean differsFrom(GridSession session) {
		return !Objects.equals(schema$Id, session.getGridJsonSchema$Id());
	}
}
