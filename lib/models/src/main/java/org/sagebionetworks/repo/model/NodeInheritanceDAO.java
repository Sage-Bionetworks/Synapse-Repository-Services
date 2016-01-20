package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This DAO controls the permission inheritance of nodes.
 * 
 * @author jmhill
 *
 */
public interface NodeInheritanceDAO {
	
	/**
	 * Get the set of nodes that currently inherited from this nodes permissions.
	 * @param benefactorId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Set<String> getBeneficiaries(String benefactorId) throws NotFoundException, DatastoreException;

	/**
	 * A node's permissions benefactor is the node which its permissions are inherited from. 
	 * @param beneficiaryId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public String getBenefactor(String beneficiaryId) throws NotFoundException, DatastoreException;
	
	
	/**
	 * Add a node as beneficiary to a given benefactor
	 * @param beneficiaryId - Each node in this list will be added as a beneficiary to the passed benefactor.
	 * @param toBenefactorId - The new permissions benefactor of the list of nodes.
	 * @throws DatastoreException 
	 */
	public void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException, DatastoreException;
	
	/**
	 * Add a node as beneficiary to a given benefactor
	 * @param beneficiaryId
	 * @param toBenefactorId
	 * @param keepOldEtag - set to true to prevent this call from changing the ETag.  This should only occur for migration.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void addBeneficiary(String beneficiaryId, String toBenefactorId, boolean keepOldEtag) throws NotFoundException, DatastoreException;
}
