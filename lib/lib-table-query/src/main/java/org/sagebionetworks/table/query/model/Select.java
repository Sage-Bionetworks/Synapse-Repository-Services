package org.sagebionetworks.table.query.model;

import java.util.List;
/**
 * The model for a select
 * @author jmhill
 *
 */
public class Select {
		
	SetQuantifier setQuantifier;
	Boolean wildcard;
	List<Column> columnList;
}
