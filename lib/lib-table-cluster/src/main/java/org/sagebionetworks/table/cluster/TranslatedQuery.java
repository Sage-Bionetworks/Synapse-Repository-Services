package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.SelectColumn;

public interface TranslatedQuery {

	List<SelectColumn> getSelectColumns();

	Map<String, ?> getParameters();

	String getOutputSQL();

	boolean getIncludesRowIdAndVersion();

	boolean getIncludeEntityEtag();

	String getSingleTableId();

}
