package org.sagebionetworks.repo.manager.table.metadata.providers;

import org.sagebionetworks.repo.model.table.ObjectField;

public class Constants {
	// @formatter:off
	public static final ObjectField[] BASIC_DEAFULT_COLUMNS = new ObjectField[] {
					ObjectField.id, 
					ObjectField.name, 
					ObjectField.description,
					ObjectField.createdOn, 
					ObjectField.createdBy,
					ObjectField.etag, 
					ObjectField.modifiedOn, 
					ObjectField.modifiedBy
	};

	static final ObjectField[] FILE_DEFAULT_COLUMNS = new ObjectField[] {
					ObjectField.id, 
					ObjectField.name, 
					ObjectField.createdOn, 
					ObjectField.createdBy,
					ObjectField.etag, 
					ObjectField.type, 
					ObjectField.currentVersion, 
					ObjectField.parentId,
					ObjectField.benefactorId, 
					ObjectField.projectId, 
					ObjectField.modifiedOn, 
					ObjectField.modifiedBy,
					ObjectField.dataFileHandleId,
					ObjectField.dataFileSizeBytes, 
					ObjectField.dataFileMD5Hex,
					ObjectField.dataFileConcreteType,
					ObjectField.dataFileBucket,
					ObjectField.dataFileKey
	};

	static final ObjectField[] DATASET_DEFAULT_COLUMNS = new ObjectField[]{
			ObjectField.datasetItemCount
	};
	// @formatter:on
}
