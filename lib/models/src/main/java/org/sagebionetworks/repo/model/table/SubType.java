package org.sagebionetworks.repo.model.table;

/**
 * 
 * Defines the sub-type of a replicated object. This is the sub-set of possible
 * EntityTypes that can appear in a view.
 */
public enum SubType {
	dockerrepo,
	entityview,
	file,
	folder,
	link,
	project,
	submission,
	submissionview,
	table,
	dataset
}
