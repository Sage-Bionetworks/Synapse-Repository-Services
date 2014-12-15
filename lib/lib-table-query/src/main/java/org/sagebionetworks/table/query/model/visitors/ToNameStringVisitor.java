package org.sagebionetworks.table.query.model.visitors;

public class ToNameStringVisitor extends ToUnquotedStringVisitor {

	public String getName() {
		return getSql();
	}
}
