package org.sagebionetworks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class AsynchronousJobWorkerHelperImpl implements AsynchronousJobWorkerHelper {

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	TableManagerSupport tableMangerSupport;
	@Autowired
	TableViewManager tableViewManager;

	@Override
	public <R extends AsynchronousRequestBody, T extends AsynchronousResponseBody> T startAndWaitForJob(UserInfo user,
			R request, long maxWaitMS, Class<? extends T> responseClass) throws InterruptedException {
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, request);
		long start = System.currentTimeMillis();
		while (!AsynchJobState.COMPLETE.equals(status.getJobState())) {
			assertFalse("Job Failed: " + status.getErrorDetails(), AsynchJobState.FAILED.equals(status.getJobState()));
			System.out.println("Waiting for job to complete: Message: " + status.getProgressMessage() + " progress: "
					+ status.getProgressCurrent() + "/" + status.getProgressTotal());
			assertTrue("Timed out waiting for job with request type: " + request.getClass().getName(),
					(System.currentTimeMillis() - start) < maxWaitMS);
			Thread.sleep(1000);
			// Get the status again
			status = this.asynchJobStatusManager.getJobStatus(user, status.getJobId());
		}
		return (T) status.getResponseBody();
	}
	
	/**
	 * Wait for EntityReplication to show the given etag for the given entityId.
	 * 
	 * @param tableId
	 * @param entityId
	 * @param etag
	 * @return
	 * @throws InterruptedException
	 */
	@Override
	public EntityDTO waitForEntityReplication(UserInfo user, String tableId, String entityId, long maxWaitMS) throws InterruptedException{
		Entity entity = entityManager.getEntity(user, entityId);
		long start = System.currentTimeMillis();
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		while(true){
			EntityDTO dto = indexDao.getEntityData(KeyFactory.stringToKey(entityId));
			if(dto == null || !dto.getEtag().equals(entity.getEtag())){
				assertTrue("Timed out waiting for table view status change.", (System.currentTimeMillis()-start) <  maxWaitMS);
				System.out.println("Waiting for entity replication. id: "+entityId+" etag: "+entity.getEtag());
				Thread.sleep(1000);
			}else{
				return dto;
			}
		}
	}
	
	/**
	 * Create a View with the default schema for its type.
	 * @param user
	 * @param name
	 * @param parentId
	 * @param schema
	 * @param scope
	 * @param viewTypeMask
	 * @return
	 */
	@Override
	public EntityView createView(UserInfo user, String name, String parentId, List<String> scope, long viewTypeMask) {
		List<ColumnModel> defaultColumns = tableMangerSupport.getDefaultTableViewColumns(viewTypeMask);
		EntityView view = new EntityView();
		view.setName(name);
		view.setViewTypeMask(viewTypeMask);
		view.setParentId(parentId);
		view.setColumnIds(TableModelUtils.getIds(defaultColumns));
		view.setScopeIds(scope);
		String viewId = entityManager.createEntity(user, view, null);
		view = entityManager.getEntity(user, viewId, EntityView.class);
		ViewScope viewScope = new ViewScope();
		viewScope.setScope(view.getScopeIds());
		viewScope.setViewTypeMask(viewTypeMask);
		tableViewManager.setViewSchemaAndScope(user, view.getColumnIds(), viewScope, viewId);
		return view;
	}

}
