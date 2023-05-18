package org.sagebionetworks.table.query.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * See: {@link CaseSpecification}.
 * <p>
 * SearchedCase ::= {@link SearchedWhenClause} [ {@link ElseClause} ]
 *
 */
public class SearchedCase extends SQLElement {
	
	private final List<SearchedWhenClause> searchedWhenClauses;
	private ElseClause elseClause;	

	public SearchedCase() {
		super();
		this.searchedWhenClauses = new ArrayList<>();
		this.elseClause = null;
	}
	
	public void addWhen(SearchedWhenClause toAdd) {
		this.searchedWhenClauses.add(toAdd);
	}
	
	public void setElse(ElseClause elseClause) {
		this.elseClause = elseClause;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		searchedWhenClauses.forEach((swc)->{
			swc.toSql(builder, parameters);
		});
		if(elseClause != null) {
			elseClause.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		List<Element> list = new LinkedList<>();
		list.addAll(searchedWhenClauses);
		if(elseClause != null) {
			list.add(elseClause);
		}
		return list;
	}

}
