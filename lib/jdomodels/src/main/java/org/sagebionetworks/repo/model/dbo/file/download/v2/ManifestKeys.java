package org.sagebionetworks.repo.model.dbo.file.download.v2;


import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ManifestKeys {

	
	ID("N."+COL_NODE_ID),
	name("N."+COL_NODE_NAME),
	versionNumber("R."+COL_REVISION_NUMBER),
	contentType("F."+COL_FILES_CONTENT_TYPE),
	dataFileSizeBytes("F."+COL_FILES_CONTENT_SIZE),
	createdBy("N."+COL_NODE_CREATED_BY),
	createdOn("N."+COL_NODE_CREATED_ON),
	modifiedBy("R."+COL_REVISION_MODIFIED_BY),
	modifiedOn("R."+COL_REVISION_MODIFIED_ON),
	parentId("N."+COL_NODE_PARENT_ID),
	synapseURL("SYNAPSE_URL"),
	dataFileMD5Hex("F."+COL_FILES_CONTENT_MD5);
	
	private String columnName;
	
	ManifestKeys(String columnName) {
		this.columnName = columnName;
	}
	
	public static final String URL = "CONCAT('https://www.synapse.org/#!Synapse:syn', "+ID.columnName+", '.', "+versionNumber.columnName+") AS SYNAPSE_URL";
	
	/**
	 * Get the column name associated with key.s
	 * @return
	 */
	public String getColumnName() {
		return this.columnName;
	}
	
	public static String buildSelect() {
		StringJoiner joiner = new StringJoiner(",");
		for(ManifestKeys key: values()) {
			if(synapseURL.equals(key)) {
				joiner.add(URL);
			}else {
				joiner.add(key.getColumnName());
			}
		}
		return joiner.toString();
	}
	
}
