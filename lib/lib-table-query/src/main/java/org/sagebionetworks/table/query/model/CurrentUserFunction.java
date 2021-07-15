package org.sagebionetworks.table.query.model;

/**
 *
 * CurrentUserFunction ::= {@link SynapseFunctionName} left_paren right_paren
 *
 */
public class CurrentUserFunction extends LeafElement implements HasFunctionReturnType{

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
    public FunctionReturnType getFunctionReturnType() {
        return synapseFunctionName.getFunctionReturnType();
    }
}