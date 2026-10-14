package org.sagebionetworks.repo.model.schema;

public class JsonSchemaConstants {
	/**
	 * The delimiter used for the path section of a $id.
	 */
	public static final String PATH_DELIMITER = "-";
	
	/**
	 * The prefix used to include a semantic version in a $id.
	 */
	public static final String VERSION_PRFIX = "-";

	/**
	 * The $id of the schema that every access requirement schema must extend. Pinned to an exact
	 * version because the form template editor hard codes it; a later version needs its own
	 * compatibility plan.
	 */
	public static final String ACCESS_REQUIREMENT_BASE_SCHEMA_ID = "org.sagebionetworks-AccessRequirementBaseSchema-1.0.0";

	/**
	 * The property of an access requirement schema that records whether a submission answers a
	 * first request or a renewal. Supplied by the system when the submission is created, so a form
	 * never collects it.
	 */
	public static final String SUBMISSION_CONTEXT_PROPERTY = "x-synapse-submissionContext";
}
