package org.sagebionetworks.table.query.model;

import java.util.List;

public class CurrentUserFunction extends SQLElement{

    SynapseFunctionName synapseFunctionName;

    public CurrentUserFunction(SynapseFunctionName synapseFunctionName){
        this.synapseFunctionName = synapseFunctionName;
    }

    @Override
    public void toSql(StringBuilder builder, ToSqlParameters parameters) {
        builder.append(synapseFunctionName.name());
        builder.append("(");
        builder.append(")");
    }

    @Override
    <T extends Element> void addElements(List<T> elements, Class<T> type) {
        // no sub-elements
    }

}