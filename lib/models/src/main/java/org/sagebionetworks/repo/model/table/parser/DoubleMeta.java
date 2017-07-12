package org.sagebionetworks.repo.model.table.parser;

/**
 * Metadata about doubles that are NaN or infinity.
 * @author John
 *
 */
public enum DoubleMeta {
	
	POSITIVE_INFINITY	(Double.POSITIVE_INFINITY	, "Infinity"	, new String[]{"inf","+inf","infinity","+infinity", "\u221E","+\u221E"}),
	NEGATIVE_INFINITY	(Double.NEGATIVE_INFINITY	, "-Infinity"	, new String[]{"-inf","-infinity","-\u221E"}),
	NAN					(Double.NaN					, "NaN"			, new String[]{"nan"});

	double value;
	String databaseName;
	String[] possibleValues;
	
	private DoubleMeta(double value, String databaseName,
			String[] possibleValues) {
		this.value = value;
		this.databaseName = databaseName;
		this.possibleValues = possibleValues;
	}
	
	/**
	 * Lookup the double matadata given a value.
	 * 
	 * @param value
	 * @return
	 * @throws IllegalArgumentException if no match is found.
	 */
	public static DoubleMeta lookupValue(String value){
		if(value == null){
			throw new IllegalArgumentException("Value is null");
		}
		value = value.toLowerCase();
		for(DoubleMeta meta: values()){
			for(String possible: meta.possibleValues){
				if(possible.equals(value)){
					return meta;
				}
			}
		}
		throw new IllegalArgumentException("No match found for: "+value);
	}
	
	/**
	 * Get the double value representation for this meatadata.
	 * @return
	 */
	public double getDoubleValue(){
		return value;
	}
	
	/**
	 * Get the database name representation for this meatadata.
	 * @return
	 */
	public String getDatabaseName(){
		return databaseName;
	}
	
}
