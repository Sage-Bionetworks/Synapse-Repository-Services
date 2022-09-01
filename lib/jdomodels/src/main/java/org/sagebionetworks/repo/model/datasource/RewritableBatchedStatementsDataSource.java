package org.sagebionetworks.repo.model.datasource;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * Special {@link BasicDataSource} for that allows to set the rewriteBatchedStatements 
 * connection property for MySQL (See https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-connp-props-performance-extensions.html)
 */
public class RewritableBatchedStatementsDataSource extends BasicDataSource {
	
	public void setRewriteBatchedStatements(boolean rewriteBatchedStatements) {
		super.addConnectionProperty("rewriteBatchedStatements", String.valueOf(rewriteBatchedStatements));
	}

}
