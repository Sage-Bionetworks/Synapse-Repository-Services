package org.sagebionetworks.repo.model.ar;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;

public interface AccessRestrictionStatusDao {

	/**
	 * For a given set of subjectIds, get the user's status for all access
	 * restriction that apply to each subject.
	 * 
	 * @param subjectIds  The ids of the subjects to fetch status information about.
	 * @param subjectType The type of subjects in the subjects list. All subjects
	 *                    must be of the same type.
	 * @param userId      The id of the user to get the status for.
	 * @param userGroups  The user's principal ids.
	 * @return The user's status for each subject in the same order as the provided
	 *         subjects.
	 */
	public List<UsersRestrictionStatus> getSubjectStatus(List<Long> subjectIds, RestrictableObjectType subjectType, Long userId,  Set<Long> userGroups);

	/**
	 * Same as: {@link #getSubjectStatus(List, RestrictableObjectType, Long, Set<Long>)()}
	 * where the type is 'ENTITY'
	 */
	public List<UsersRestrictionStatus> getEntityStatus(List<Long> entityIds, Long userId,  Set<Long> userGroups);
	
	/**
	 * Same as: {@link #getSubjectStatus(List, RestrictableObjectType, Long, Set<Long>)()}
	 * where the type is 'ENTITY'
	 * @return Returns the results as a map of EntityID to UsersRestrictionStatus.
	 */
	public Map<Long, UsersRestrictionStatus> getEntityStatusAsMap(List<Long> entityIds, Long userId, Set<Long> userGroups);

	/**
	 * 	 * For a given set of subjectIds, get the user's status for all access
	 * 	 * restriction that apply to each subject.
	 * 	 *
	 * 	 * @param subjectIds  The ids of the subjects to fetch status information about.
	 * 	 * @param subjectType The type of subjects in the subjects list. where the type is not an 'ENTITY'
	 * 	 * @param userId      The id of the user to get the status for.
	 * 	 * @return The user's status for each subject in the same order as the provided
	 * 	 *         subjects.
	 */
	public List<UsersRestrictionStatus> getNonEntityStatus(List<Long> subjectIds, RestrictableObjectType subjectType, Long userId);
}
