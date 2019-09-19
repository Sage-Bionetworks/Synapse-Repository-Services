package org.sagebionetworks.repo.model.dbo.form;

import java.util.Optional;

import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.model.form.SubmissionStatus;

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

	/**
	 * Create a FormData object.
	 * @param creatorId Id of the creator.
	 * @param groupId Id of the group for the form.
	 * @param name Name of the form data.
	 * @param dataFileHandleId FileHandle Id of the actual form data.
	 * @return
	 */
	public FormData createFormData(Long creatorId, String groupId, String name, String dataFileHandleId);

	/**
	 * Get the creator of the identified FormData.
	 * @param id
	 * @return
	 */
	public long getFormDataCreator(String formDataId);

	/**
	 * Get the groupId for the identified FormData
	 * @param id
	 * @return
	 */
	public String getFormDataGroupId(String formDataId);

	/**
	 * Update the name and dataFileHandleId for the identified FormData
	 * @param id
	 * @param name
	 * @param dataFileHandleId
	 * @return
	 */
	public FormData updateFormData(String formDataId, String name, String dataFileHandleId);

	/**
	 * Update the name and dataFileHandleId for the identified FormData
	 * @param id
	 * @param dataFileHandleId
	 * @return
	 */
	public FormData updateFormData(String formDataId, String dataFileHandleId);

	/**
	 * Get the current state for the identified FormData
	 * @param id
	 * @return
	 */
	public StateEnum getFormDataState(String formDataId);
	
	/**
	 * Get the current status for the identified FormData
	 * @param id
	 * @return
	 */
	public SubmissionStatus getFormDataStatus(String formDataId);
	
	/**
	 * Get the the identified FormData
	 * @param id
	 * @return
	 */
	public FormData getFormData(String formDataId);
	
	/**
	 * Truncate all data.
	 */
	public void truncateAll();

	/**
	 * Delete the identified FormData
	 * @param formDataId
	 */
	public boolean deleteFormData(String formDataId);

	/**
	 * Submit a FormData for submission.
	 * @param formDataId
	 * @return
	 */
	public FormData updateStatus(String formDataId, SubmissionStatus status);
}
