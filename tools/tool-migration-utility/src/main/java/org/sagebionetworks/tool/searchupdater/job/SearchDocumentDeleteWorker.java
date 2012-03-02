package org.sagebionetworks.tool.searchupdater.job;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.job.WorkerResult;
import org.sagebionetworks.tool.searchupdater.CloudSearchClient;
import org.sagebionetworks.tool.searchupdater.SearchUpdaterConfigurationImpl;

/**
 * A worker that will execute a single CloudSearch delete entity job.
 * 
 * @author deflaux
 * 
 */
public class SearchDocumentDeleteWorker implements Callable<WorkerResult> {

	static private Log log = LogFactory
			.getLog(SearchDocumentDeleteWorker.class);

	SearchUpdaterConfigurationImpl configuration = null;
	Set<String> entities = null;
	BasicProgress progress = null;

	/**
	 * Create a new delete worker
	 * 
	 * @param configuration
	 * @param clientFactory
	 * @param entities
	 * @param progress
	 */
	public SearchDocumentDeleteWorker(Configuration configuration,
			ClientFactory clientFactory, Set<String> entities,
			BasicProgress progress) {
		super();
		this.configuration = (SearchUpdaterConfigurationImpl) configuration;
		this.entities = entities;
		this.progress = progress;
	}

	@Override
	public WorkerResult call() throws Exception {
		try {
			// Note that we cannot use a JSONEntity here because the format is
			// just a JSON array
			JSONArray documentBatch = new JSONArray();
			for (String entityId : this.entities) {
				Document document = new Document();
				document.setType(DocumentTypeNames.delete);
				document.setId(entityId);
				document.setVersion(new Long(Integer.MAX_VALUE));
				document.setLang("en"); // TODO this should have been set via "default" in the schema for this
				documentBatch.put(EntityFactory.createJSONObjectForEntity(document));
			}

			log.info("Deleting from search index "
					+ documentBatch.toString(4));

			// TODO need an extra safety check here
			
			CloudSearchClient csClient = configuration.createCloudSearchClient();
			csClient.sendDocuments(documentBatch.toString());

			progress.setCurrent(progress.getCurrent() + this.entities.size());
			Thread.sleep(1000);
			progress.setDone();
			return new WorkerResult(this.entities.size(),
					WorkerResult.JobStatus.SUCCEDED);
		} catch (Exception e) {
			// done
			progress.setDone();
			// Log any errors
			log.error("SearchDocumentDeleteWorker Failed to run job: "
					+ entities.toString(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}

}
