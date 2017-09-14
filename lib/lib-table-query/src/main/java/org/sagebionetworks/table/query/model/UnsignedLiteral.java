package org.sagebionetworks.table.query.model;

import java.util.List;

public class UnsignedLiteral extends SQLElement implements HasQuoteValue  {
	
	private String overrideSql;
	private UnsignedNumericLiteral unsignedNumericLiteral;
	private GeneralLiteral generalLiteral;
	
	public UnsignedLiteral(UnsignedNumericLiteral unsignedNumericLiteral) {
		super();
		this.unsignedNumericLiteral = unsignedNumericLiteral;
	}
	
	public UnsignedLiteral(GeneralLiteral generalLiteral) {
		super();
		this.generalLiteral = generalLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(overrideSql != null){
			builder.append(overrideSql);
		}else if(unsignedNumericLiteral != null){
			unsignedNumericLiteral.toSql(builder);
		} else {
			generalLiteral.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedNumericLiteral);
		checkElement(elements, type, generalLiteral);
	}

	@Override
	public String getValueWithoutQuotes() {
		if(unsignedNumericLiteral != null){
			return unsignedNumericLiteral.toSql();
		}else{
			return generalLiteral.getValueWithoutQuotes();
		}
	}

	@Override
	public boolean isSurrounedeWithQuotes() {
		return generalLiteral != null;
	}

	@Override
	public void overrideSql(String overrideSql) {
		this.overrideSql = overrideSql;
	}
}
