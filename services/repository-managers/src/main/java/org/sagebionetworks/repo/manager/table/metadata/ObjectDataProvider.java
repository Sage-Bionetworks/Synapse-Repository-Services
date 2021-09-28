package org.sagebionetworks.repo.manager.table.metadata;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;

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
	 * Get the replication type for this provider.
	 * 
	 * @return
	 */
	ReplicationType getReplicationType();


	/**
	 * Provide a stream of IdAndChecksum data for the given parentIds and subTypes
	 * ordered by the IDs ascending. The checksum must include all version of the
	 * objects that match the condition. See the following pusdo-sql:
	 * </p>
	 * <code>SELECT ID, SUM(CRC32(CONCAT(salt','-',ETAG,'-',VERSION,'-',BENEFACTOR_ID))) AS CHECK_SUM ... GROUP BY ID ORDER BY ID ASC</code>
	 * 
	 * @param parentIds
	 * @param subTypes
	 * @param limit
	 * @param offset
	 * @return
	 */
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForChildren(Long salt, Set<Long> parentIds, Set<SubType> subTypes);

	/**
	 * Provide a stream of IdAndChecksum data for the given objectIds ordered by the
	 * IDs ascending. The checksum must include all version of the objects that
	 * match the condition. See the following pusdo-sql:
	 * </p>
	 * <code>SELECT ID, SUM(CRC32(CONCAT(salt','-',ETAG,'-',VERSION,'-',BENEFACTOR_ID))) AS CHECK_SUM ... GROUP BY ID ORDER BY ID ASC</code>
	 * 
	 * @param objectIds
	 * @param subTypes
	 * @param limit
	 * @param offset
	 * @return
	 */
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForObjects(Long salt, Set<Long> objectIds);

}
