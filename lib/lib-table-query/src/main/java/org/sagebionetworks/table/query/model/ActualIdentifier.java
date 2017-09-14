package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltactual identifier&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ActualIdentifier extends SQLElement implements HasQuoteValue {
	
	String overrideSql = null;
	RegularIdentifier regularIdentifier;
	DelimitedIdentifier delimitedIdentifier;
	
	public ActualIdentifier(RegularIdentifier regularIdentifier) {
		super();
		this.regularIdentifier = regularIdentifier;
	}
	
	public ActualIdentifier(DelimitedIdentifier delimitedIdentifier) {
		super();
		this.delimitedIdentifier = delimitedIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(overrideSql != null){
			builder.append(overrideSql);
		}else if(regularIdentifier != null){
			regularIdentifier.toSql(builder);
		}else{
			delimitedIdentifier.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, regularIdentifier);
		checkElement(elements, type, delimitedIdentifier);
	}

	@Override
	public String getValueWithoutQuotes() {
		if(regularIdentifier != null){
			return regularIdentifier.toSql();
		}else{
			return delimitedIdentifier.getValueWithoutQuotes();
		}
	}

	@Override
	public boolean isSurrounedeWithQuotes() {
		return delimitedIdentifier != null;
	}

	@Override
	public void overrideSql(String overrideSql) {
		this.overrideSql = overrideSql;
	}

}
