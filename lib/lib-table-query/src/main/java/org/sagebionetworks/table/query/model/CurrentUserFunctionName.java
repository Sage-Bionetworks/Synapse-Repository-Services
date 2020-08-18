package org.sagebionetworks.table.query.model;

public enum CurrentUserFunctionName {
    // date-time
    CURRENT_USER(FunctionReturnType.STRING);

    FunctionReturnType returnType;

    CurrentUserFunctionName(FunctionReturnType returnType){
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
