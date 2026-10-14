package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

public interface GridRecordSetExporter {

	/**
	 * Export the current grid contents to a new RecordSet version by re-reading the
	 * grid.
	 *
	 * @deprecated This standalone path re-reads the grid and depends on the
	 *             asynchronous merge/validation patches having settled. Prefer
	 *             PULL_PUSH synchronization (see
	 *             {@code GridSynchronizationManager}), which builds the pushed
	 *             version synchronously during the merge. Retained for backward
	 *             compatibility only.
	 */
	@Deprecated
	GridRecordSetExportResponse exportGrid(UserInfo user, GridRecordSetExportRequest request, AsyncJobProgressCallback jobCallback);

	/**
	 * Create a {@link RecordSetArtifactBuilder} that buffers, validates, and writes surviving
	 * grid rows to a new RecordSet's data CSV and validation-details CSV.
	 *
	 * @param fileNamePrefix     prefix for the temp files (e.g. the job id)
	 * @param orderedColumnNames the final schema column names in CSV output order
	 * @param validationSchema   the de-referenced validation schema, or null when
	 *                           the RecordSet has no bound schema
	 * @param csvDescriptor      the RecordSet's CSV descriptor (separator etc.)
	 * @param recordSetId        the RecordSet id
	 * @return a new builder; the caller is responsible for closing it
	 */
	RecordSetArtifactBuilder createArtifactBuilder(String fileNamePrefix, List<String> orderedColumnNames, JsonSchema validationSchema,
	                                               CsvTableDescriptor csvDescriptor, String recordSetId) throws IOException;

	/**
	 * Upload the artifacts accumulated by a {@link RecordSetArtifactBuilder#finish() finished}
	 * {@link RecordSetArtifactBuilder} and create a new RecordSet version from them. The
	 * builder must be finished before this call so its temp files are complete and its validation
	 * summary is final; the caller retains ownership and is responsible for closing the builder
	 * afterward.
	 *
	 * @param user      the calling user
	 * @param recordSet the source RecordSet to create a new version of
	 * @param artifactBuilder      the finished artifactBuilder holding the data CSV, validation-details CSV,
	 *                  and validation summary
	 * @return the updated RecordSet (with the new version number)
	 */
	RecordSet pushFromArtifactBuilder(UserInfo user, RecordSet recordSet, RecordSetArtifactBuilder artifactBuilder);

}
