package org.sagebionetworks.table.query.model;

public class ArrayHasLikeSpec {
	
	private EscapeCharacter escapeCharacter;
	
	public ArrayHasLikeSpec() {	
		this(null);
	}

	public ArrayHasLikeSpec(EscapeCharacter escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}
	
	public EscapeCharacter getEscapeCharacter() {
		return escapeCharacter;
	}

}
