package org.sagebionetworks.repo.model.dbo.form;

import java.util.Optional;

import org.sagebionetworks.repo.model.form.FormGroup;

public interface FormDao {

	/**
	 * Create a new FormGroup.
	 * 
	 * @param creator
	 * @param name
	 * @return
	 */
	public FormGroup createFormGroup(Long creator, String name);
	
	/**
	 * Lookup the form group for the given name.
	 * 
	 * @param name
	 * @return Optional.empty() if a group does not exist for the given name
	 */
	public Optional<FormGroup> lookupGroupByName(String name);
}
