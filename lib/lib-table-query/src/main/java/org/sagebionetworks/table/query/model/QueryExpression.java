package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * QueryExpression ::= [ WITH {@link WithListElement} ( <comma> {@link WithListElement} )* ] {@link NonJoinQueryExpression}
 * <p>
 * A modified form of the following definition:
 * 
 * @see <a href=
 *      "http://teiid.github.io/teiid-documents/9.0.x/content/reference/BNF_for_SQL_Grammar.html#queryExpression">QueryExpression</a>
 * 
 */
public class QueryExpression extends SQLElement implements HasSqlContext {

	private SqlContext sqlContext;
	private final List<WithListElement> withListElements;
	private final NonJoinQueryExpression nonJoinQueryExpression;

	public QueryExpression(List<WithListElement> withListElements, NonJoinQueryExpression nonJoinQueryExpression) {
		this.withListElements = withListElements;
		this.nonJoinQueryExpression = nonJoinQueryExpression;
		this.sqlContext = SqlContext.query;
		this.recursiveSetParent();
	}

	@Override
	public SqlContext getSqlContext() {
		return sqlContext;
	}

	public void setSqlContext(SqlContext context) {
		this.sqlContext = context;
	}
	
	public Optional<List<WithListElement>> getWithListElements(){
		if(this.withListElements == null || this.withListElements.isEmpty()) {
			return Optional.empty();
		}else {
			return Optional.of(withListElements);
		}
	}

	public NonJoinQueryExpression getNonJoinQueryExpression() {
		return nonJoinQueryExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(withListElements != null && !withListElements.isEmpty()) {
			builder.append("WITH ");
			for(int i=0; i<withListElements.size(); i++) {
				if(i>0) {
					builder.append(", ");
				}
				withListElements.get(i).toSql(builder, parameters);
			}
			builder.append(" ");
		}
		nonJoinQueryExpression.toSql(builder, parameters);
		
	}

	@Override
	public Iterable<Element> getChildren() {
		List<Element> list = new LinkedList<>();
		list.add(nonJoinQueryExpression);
		if(this.withListElements != null) {
			list.addAll(withListElements);
		}
		return list;
	}

}
