package org.sagebionetworks.table.query.model;

import java.util.List;

public class CurrentUserFunction extends SQLElement{

    CurrentUserFunctionName currentUserFunctionName;

    public CurrentUserFunction(CurrentUserFunctionName currentUserFunctionName){
        this.currentUserFunctionName = currentUserFunctionName;
    }

    @Override
    public void toSql(StringBuilder builder, ToSqlParameters parameters) {
        builder.append(currentUserFunctionName.name());
        builder.append("(");
        builder.append(")");
    }

    @Override
    <T extends Element> void addElements(List<T> elements, Class<T> type) {
        // no sub-elements
    }

}