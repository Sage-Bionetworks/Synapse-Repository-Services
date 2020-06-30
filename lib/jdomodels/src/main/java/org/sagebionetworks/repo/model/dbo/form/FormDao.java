package org.sagebionetworks.repo.model.dbo.form;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.ListRequest;
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
	 * 
	 * @param creatorId        Id of the creator.
	 * @param groupId          Id of the group for the form.
	 * @param name             Name of the form data.
	 * @param dataFileHandleId FileHandle Id of the actual form data.
	 * @return
	 */
	public FormData createFormData(Long creatorId, String groupId, String name, String dataFileHandleId);

	/**
	 * Get the creator of the identified FormData.
	 * 
	 * @param id
	 * @return
	 */
	public long getFormDataCreator(String formDataId);

	/**
	 * Get the groupId for the identified FormData
	 * 
	 * @param id
	 * @return
	 */
	public String getFormDataGroupId(String formDataId);

	/**
	 * Update the name and dataFileHandleId for the identified FormData
	 * 
	 * @param id
	 * @param name
	 * @param dataFileHandleId
	 * @return
	 */
	public FormData updateFormData(String formDataId, String name, String dataFileHandleId);

	/**
	 * Update the name and dataFileHandleId for the identified FormData
	 * 
	 * @param id
	 * @param dataFileHandleId
	 * @return
	 */
	public FormData updateFormData(String formDataId, String dataFileHandleId);

	/**
	 * Get the current state for the identified FormData
	 * 
	 * @param id
	 * @return
	 */
	public StateEnum getFormDataState(String formDataId);

	/**
	 * Get the current status for the identified FormData
	 * 
	 * @param id
	 * @return
	 */
	public SubmissionStatus getFormDataStatusForUpdate(String formDataId);

	/**
	 * Get the the identified FormData
	 * 
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
	 * 
	 * @param formDataId
	 * @return True if an update occurred.
	 */
	public boolean deleteFormData(String formDataId);

	/**
	 * Submit a FormData for submission.
	 * 
	 * @param formDataId
	 * @return
	 */
	public FormData updateStatus(String formDataId, SubmissionStatus status);

	/**
	 * List of FormData objects filtered by the creator.
	 * 
	 * The results are limited to the provided group.
	 * 
	 * @param creatorId Only objects created by this user will be returned.
	 * @param request
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<FormData> listFormDataByCreator(Long creatorId, ListRequest request, long limit, long offset);

	/**
	 * List of FormData objects for a reviewer. The results are limited to the
	 * provided group.
	 * 
	 * @param request Filter details
	 * @param limitForQuery
	 * @param offset
	 * @return
	 */
	public List<FormData> listFormDataForReviewer(ListRequest request, long limit, long offset);

	/**
	 * Get the fileHandleId for the identified FormData.
	 * @param formDataId
	 * @return
	 */
	public String getFormDataFileHandleId(String formDataId);

	/**
	 * Get the identified FormGroup.
	 * 
	 * @param id
	 * @return
	 */
	public FormGroup getFormGroup(String id);
}
