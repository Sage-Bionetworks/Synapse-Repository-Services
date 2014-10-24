package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltsort specification list&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SortSpecificationList extends SQLElement {
	
	List<SortSpecification> sortSpecifications;

	public SortSpecificationList() {
		super();
		this.sortSpecifications = new LinkedList<SortSpecification>();;
	}
	
	public SortSpecificationList(List<SortSpecification> list) {
		this.sortSpecifications = list;
	}

	public void addSortSpecification(SortSpecification sortSpecification){
		this.sortSpecifications.add(sortSpecification);
	}

	public List<SortSpecification> getSortSpecifications() {
		return sortSpecifications;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		boolean first = true;
		for(SortSpecification sortSpecification: sortSpecifications){
			if(!first){
				builder.append(", ");
			}
			sortSpecification.toSQL(builder, columnConvertor);
			first = false;
		}
	}

}
