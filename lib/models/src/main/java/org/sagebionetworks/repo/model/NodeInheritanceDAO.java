package org.sagebionetworks.repo.model;

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
	 */
	public Set<String> getBeneficiaries(String benefactorId) throws NotFoundException;

	/**
	 * A node's permissions benefactor is the node which its permissions are inherited from. 
	 * @param beneficiaryId
	 * @return
	 * @throws NotFoundException
	 */
	public String getBenefactor(String beneficiaryId) throws NotFoundException;
	
	/**
	 * Add a node as beneficiary to a given benefactor
	 * @param beneficiaryId - Each node in this list will be added as a beneficiary to the passed benefactor.
	 * @param toBenefactorId - The new permissions benefactor of the list of nodes.
	 */
	public void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException;
}
