package org.sagebionetworks.tool.migration;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.QueryRunner;

/**
 * A worker that runs a long running query.
 * @author jmhill
 *
 */
public class AllEntityDataWorker<T> implements Callable<List<T>>{
	
	// This is used to keep track of the progress.
	private BasicProgress progress;
	private QueryRunner<T> queryRunner;
	
	

	/**
	 * Create a new worker.
	 * @param client
	 * @param queryRunner
	 * @param progress
	 */
	public AllEntityDataWorker(QueryRunner<T> queryRunner,
			BasicProgress progress) {
		super();
		this.queryRunner = queryRunner;
		this.progress = progress;
	}



	@Override
	public List<T> call() throws Exception {
		// Execute the query
		return queryRunner.getAllEntityData(progress);
	}

}
