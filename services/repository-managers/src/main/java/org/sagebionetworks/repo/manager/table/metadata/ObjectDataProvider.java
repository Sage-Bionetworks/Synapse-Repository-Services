package org.sagebionetworks.repo.manager.table.metadata;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;

public interface ObjectDataProvider {

	/**
	 * Fetch the {@link ObjectDataDTO} for the objects with the given ids, the DTO
	 * will have to include the annotations on the object. The annotations might
	 * contain any kind of property that should be indexed and exposed as a view
	 * (e.g. including object attributes that are not part of the standard
	 * replication fields).
	 * 
	 * @param objectIds          The list of object identifiers
	 * @param maxAnnotationChars The maximum number of chars of the value(s)
	 *                           represented as string, should truncate the values
	 *                           to that size
	 * @return
	 */
	Iterator<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars);

	/**
	 * Returns the sub-set of available containers, a container is available if it
	 * exists and it's not trashed
	 * 
	 * @param containerIds
	 * @return The
	 */
	Set<Long> getAvailableContainers(List<Long> containerIds);
	
	
	/**
	 * For each container id (e.g. ids that are allowed in the scope of the view)
	 * get the sum of CRCs of their children.
	 * 
	 * <p>
	 * In general this can be computed using the CRC32 of the CONCAT of ID, ETAG and
	 * BENEFACTOR grouping by the container id:
	 * <p>
	 * SELECT PARENT_ID, SUM(CRC32(CONCAT(ID,'-',ETAG,'-', BENEFACTOR_ID))) AS 'CRC'
	 * FROM TABLE WHERE PARENT_ID IN(:parentId) GROUP BY PARENT_ID
	 * 
	 * @param containerIds
	 * @return Map.key = containerId and map.value = sum of children CRCs
	 */
	Map<Long, Long> getSumOfChildCRCsForEachContainer(List<Long> containerIds);
	
	/**
	 * Get the replication type for this provider.
	 * @return
	 */
	ReplicationType getReplicationType();
	
	
	/**
	 * For the given container id return the <id, etag, benefactor> of the direct
	 * children
	 * 
	 * @param containerId
	 * @return The list of children metadata including the id, etag and benefactor
	 */
	List<IdAndEtag> getChildren(Long containerId);
	
}
