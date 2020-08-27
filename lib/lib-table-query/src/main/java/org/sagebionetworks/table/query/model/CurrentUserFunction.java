package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 *
 * CurrentUserFunction ::= {@link SynapseFunctionName} left_paren right_paren
 *
 */
public class CurrentUserFunction extends SQLElement implements HasFunctionReturnType{

    private SynapseFunctionName synapseFunctionName;

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

    @Override
    public FunctionReturnType getFunctionReturnType() {
        return synapseFunctionName.getFunctionReturnType();
    }
}