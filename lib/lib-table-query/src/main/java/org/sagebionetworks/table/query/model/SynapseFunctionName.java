package org.sagebionetworks.table.query.model;

/**
 * Supported Synapse Functions
 */
public enum SynapseFunctionName {
    // User Id
    CURRENT_USER(FunctionReturnType.LONG);

    FunctionReturnType returnType;

    SynapseFunctionName(FunctionReturnType returnType){
        this.returnType = returnType;
    }

    /**
     * The return type of this function.
     * @return
     */
    public FunctionReturnType getFunctionReturnType(){
        return this.returnType;
    }
}
