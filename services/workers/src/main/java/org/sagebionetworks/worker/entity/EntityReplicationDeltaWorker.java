package org.sagebionetworks.worker.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.common.util.progress.ThrottlingProgressCallback;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Entity replication data is normally, kept in-synch with the truth by the
 * {@link EntityReplicationWorker} which replicates entity data by listening to
 * entity change events. However, message delivery is not guaranteed so a
 * secondary process is needed to ensure the entity replication data is kept
 * up-to-date with the truth. This worker acts as the backup replication process
 * by scanning for deltas between the truth and the replicated data.
 * Specifically, this worker scans for deltas by comparing check-sums (cyclic
 * redundancy check CRC32) between the truth and the replicated data.
 * 
 */
public class EntityReplicationDeltaWorker implements ProgressingRunner<Void> {
	
	public static final long THROTTLE_FREQUENCY_MS = 1000*30;
	static Long MAX_PAGE_SIZE = 10000L;
	static private Logger log = LogManager.getLogger(EntityReplicationDeltaWorker.class);
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	ConnectionFactory connectionFactory;


	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		// wrap the callback to throttle
		progressCallback = new ThrottlingProgressCallback<Void>(progressCallback, THROTTLE_FREQUENCY_MS);
		// All parentIds that are in the trash need to be ignored.
		Set<Long> trashedParents = new HashSet<Long>(
				nodeDao.getAllContainerIds(StackConfiguration.getTrashFolderEntityIdStatic()));
				
		long limit = MAX_PAGE_SIZE;
		long offset = 0L;
		// process one page of parentIds at a time.
		List<Long> parentIds = nodeDao.getParenIds(limit, offset);
		while(!parentIds.isEmpty()){
			// make progress between each page
			progressCallback.progressMade(null);
			findDeltas(progressCallback, parentIds, trashedParents);
			// get the next page
			offset = offset+limit;
			parentIds = nodeDao.getParenIds(limit, offset);
		}
	}
	
	/**
	 * Find the deltas for one page of parentIds.
	 * @param progressCallback
	 * @param parentIds
	 */
	public void findDeltas(ProgressCallback<Void> progressCallback, List<Long> parentIds, Set<Long> trashedParents){
		// Synch with each index.
		List<TableIndexDAO> indexDaos = connectionFactory.getAllConnections();
		Collections.shuffle(indexDaos);
		TableIndexDAO firstIndex = indexDaos.get(0);
		// Find the parents out-of-synch.
		Set<Long> outOfSynchParentIds = compareCheckSums(progressCallback, parentIds, firstIndex, trashedParents);
		log.info("Out-of-synch parents: "+outOfSynchParentIds.size());
		List<ChangeMessages> changes = new LinkedList<ChangeMessages>();
		for(Long outOfSynchParentId: outOfSynchParentIds){
			boolean isParentInTrash = trashedParents.contains(outOfSynchParentId);
			List<ChangeMessages> childChanges = findChangesForParentId(progressCallback, firstIndex, outOfSynchParentId, isParentInTrash);
			changes.addAll(childChanges);
		}
		// send all of the changes..
		ff
		
	}

	/**
	 * Create the changes for a parentId that is out of Synch.
	 * @param firstIndex
	 * @param outOfSynchParentId
	 * @param isParentInTrash
	 * @return
	 */
	private List<ChangeMessages> findChangesForParentId(
			ProgressCallback<Void> progressCallback,
			TableIndexDAO firstIndex, Long outOfSynchParentId,
			boolean isParentInTrash) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Compare the check-sums between the truth and the index for the given parentIds.
	 * @param progressCallback
	 * @param parentIds
	 * @param truthCRCs
	 * @param indexDao
	 * @param trashedParents
	 * @return
	 */
	public Set<Long> compareCheckSums(ProgressCallback<Void> progressCallback,
			List<Long> parentIds,
			TableIndexDAO indexDao, Set<Long> trashedParents) {
		Map<Long, Long> truthCRCs = nodeDao.getParentCRCs(parentIds);
		Map<Long, Long> indexCRCs = indexDao.getParentCRCs(parentIds);
		HashSet<Long> parentsOutOfSynch = new HashSet<Long>();
		// Find the mismatches
		for(Long parentId: truthCRCs.keySet()){
			progressCallback.progressMade(null);
			Long truthCRC = truthCRCs.get(parentId);
			Long indexCRC = indexCRCs.get(parentId);
			if(!truthCRC.equals(indexCRC)){
				// The CRCs do not match
				if(!trashedParents.contains(truthCRC)){
					// The parent is not in the trash
					parentsOutOfSynch.add(parentId);
				}else{
					// the parent is in the trash.
					if(indexCRC != null){
						parentsOutOfSynch.add(parentId);
					}
				}
			}
		}
		return parentsOutOfSynch;
	}

}
