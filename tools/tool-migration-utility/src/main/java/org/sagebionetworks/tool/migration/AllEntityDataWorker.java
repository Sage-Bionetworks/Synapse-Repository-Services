package org.sagebionetworks.tool.migration;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;

/**
 * A worker that runs a long running query.
 * @author jmhill
 *
 */
public class AllEntityDataWorker implements Callable<List<EntityData>>{
	
	// This is used to keep track of the progress.
	private BasicProgress progress;
	private QueryRunner queryRunner;
	
	

	/**
	 * Create a new worker.
	 * @param client
	 * @param queryRunner
	 * @param progress
	 */
	public AllEntityDataWorker(QueryRunner queryRunner,
			BasicProgress progress) {
		super();
		this.queryRunner = queryRunner;
		this.progress = progress;
	}



	@Override
	public List<EntityData> call() throws Exception {
		// Execute the query
		return queryRunner.getAllEntityData(progress);
	}

}
