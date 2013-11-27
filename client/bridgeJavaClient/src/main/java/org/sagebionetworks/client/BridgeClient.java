package org.sagebionetworks.client;

import java.util.List;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for Synapse.
 * 
 * @author jmhill
 * 
 */
public interface BridgeClient extends BaseClient {

	/**
	 * Get the endpoint of the bridge service
	 */
	public String getBridgeEndpoint();

	/**
	 * The repository endpoint includes the host and version. For example: "https://repo-prod.prod.sagebase.org/repo/v1"
	 */
	public void setBridgeEndpoint(String repoEndpoint);

	/****** general bridge info ******/

	/**
	 * get version info for bridge stack
	 * 
	 * @return
	 * @throws SynapseException
	 */
	public BridgeVersionInfo getBridgeVersionInfo() throws SynapseException;

	/****** communities ******/

	/**
	 * create a new community
	 * 
	 * @param community the template to create a new community from
	 * @return the newly created community
	 * @throws SynapseException
	 */
	public Community createCommunity(Community community) throws SynapseException;

	/**
	 * Get the communities for the current user
	 * 
	 * @throws SynapseException
	 */
	public PaginatedResults<Community> getCommunities(long limit, long offset) throws SynapseException;

	/**
	 * Get all the available communities
	 * 
	 * @throws SynapseException
	 */
	public PaginatedResults<Community> getAllCommunities(long limit, long offset) throws SynapseException;

	/**
	 * Get community by id
	 * 
	 * @throws SynapseException
	 */
	public Community getCommunity(String communityId) throws SynapseException;

	/**
	 * Update community information
	 * 
	 * @param community
	 * @return
	 * @throws SynapseException
	 */
	public Community updateCommunity(Community community) throws SynapseException;

	/**
	 * Delete a community
	 * 
	 * @param communityId
	 * @return
	 * @throws SynapseException
	 */
	public void deleteCommunity(String communityId) throws SynapseException;

	/**
	 * Get all the members of a community (admins only)
	 * 
	 * @return
	 * @throws SynapseException
	 */
	public PaginatedResults<UserGroupHeader> getCommunityMembers(String communityId, long limit, long offset) throws SynapseException;

	/**
	 * Join a community
	 * 
	 * @param community
	 * @throws SynapseException
	 */
	public void joinCommunity(String communityId) throws SynapseException;

	/**
	 * Leave a community
	 * 
	 * @param community
	 * @throws SynapseException
	 */
	public void leaveCommunity(String communityId) throws SynapseException;

	/**
	 * Add a current member as an admin for the community
	 * 
	 * @param communityId
	 * @param memberName
	 * @throws SynapseException
	 */
	public void addCommunityAdmin(String communityId, String memberName) throws SynapseException;

	/**
	 * Remove a current member as an admin for the community
	 * 
	 * @param communityId
	 * @param memberName
	 * @throws SynapseException
	 */
	public void removeCommunityAdmin(String communityId, String memberName) throws SynapseException;
}
