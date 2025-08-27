package org.sagebionetworks.repo.model.grid.patch.operation.builder;

/**
 * Utility class for creating instances of various operations.
 * This class provides static methods to create builders for different types of operations.
 * Each method returns a builder that can be used to construct the operation with specific parameters.
 */
public final class Operations {

    private Operations() {
        // Private constructor to prevent instantiation
    }

    public static NewConstantBuilder newConstant() {
        return new NewConstantBuilder();
    }

    public static NewValueBuilder newValue() {
        return new NewValueBuilder();
    }

    public static NewObjectBuilder newObject() {
        return new NewObjectBuilder();
    }

    public static NewVectorBuilder newVector() {
        return new NewVectorBuilder();
    }

    public static NewArrayBuilder newArray() {
        return new NewArrayBuilder();
    }

    public static InsertValueBuilder insertValue() {
        return new InsertValueBuilder();
    }

    public static InsertObjectBuilder insertObject() {
        return new InsertObjectBuilder();
    }

    public static InsertVectorBuilder insertVector() {
        return new InsertVectorBuilder();
    }

    public static InsertArrayBuilder insertArray() {
        return new InsertArrayBuilder();
    }
    
    public static DeleteBuilder delete() {
		return new DeleteBuilder();
	}
}