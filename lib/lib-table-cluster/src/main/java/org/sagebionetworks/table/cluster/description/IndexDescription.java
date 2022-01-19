package org.sagebionetworks.table.cluster.description;

import java.util.List;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

public interface IndexDescription {
	
	/**
	 * The IdAndVersion of this table/view
	 * @return
	 */
	IdAndVersion getIdAndVersion();
	
	/**
	 * The SQL statement to create or update the index for this table/view
	 * @return
	 */
	String getCreateOrUpdateIndexSql();
	
	/**
	 * The name of each benefactor column in this table/view.
	 * @return Will return an empty if there are no benefactors.s
	 */
	List<String> getBenefactorColumnNames();

}
