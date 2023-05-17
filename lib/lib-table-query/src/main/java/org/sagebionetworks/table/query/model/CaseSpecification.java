package org.sagebionetworks.table.query.model;

/**
 * Note: In sql-92.bnf this object is defined as:
 * <p>
 * CaseSpecification ::= {@link SimpleCase} | {@link SearchedCase}
 * 
 * However, both SimleCase and SearchedCase started with CASE and end with END which is ambiguous.
 * To resolve this ambiguity we redefined this object as:
 * <p>
 *  CaseSpecification ::= <CASE> ( {@link SimpleCase} | {@link SearchedCase} ) <END>
 *
 */
public class CaseSpecification extends SQLElement {
	
	private final SimpleCase simpleCase;
	private final SearchedCase searchedCase;

	public CaseSpecification(SimpleCase simpleCase) {
		this.simpleCase = simpleCase;
		this.searchedCase = null;
	}
	
	public CaseSpecification(SearchedCase searchedCase) {
		this.simpleCase = null;
		this.searchedCase = searchedCase;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("CASE ");
		if(simpleCase != null) {
			simpleCase.toSql(builder, parameters);
		}
		if(searchedCase != null) {
			searchedCase.toSql(builder, parameters);
		}
		builder.append(" END");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(simpleCase, searchedCase);
	}

}
