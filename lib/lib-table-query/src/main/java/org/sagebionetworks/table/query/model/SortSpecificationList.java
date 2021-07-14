package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltsort specification list&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		boolean first = true;
		for(SortSpecification sortSpecification: sortSpecifications){
			if(!first){
				builder.append(", ");
			}
			sortSpecification.toSql(builder, parameters);
			first = false;
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		for(SortSpecification sortSpecification: sortSpecifications){
			checkElement(elements, type, sortSpecification);
		}
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(sortSpecifications);
	}
}
