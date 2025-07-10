package org.sagebionetworks.repo.model.grid.patch;

/**
 * The value of a con node is JSON-like value. The value can be any JSON value,
 * including null, true, false, numbers, strings, arrays, objects, binary blobs,
 * undefined value, and logical clock timestamp
 */
public enum ConType {

	NULL, BOOLEAN, LONG, DOUBLE, STRING, JSON_ARRAY, JSON_OBJECT, UNDEFINED, TIMESTAMP;
}
