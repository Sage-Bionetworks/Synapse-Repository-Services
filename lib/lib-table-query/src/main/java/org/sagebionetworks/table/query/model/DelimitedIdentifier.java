package org.sagebionetworks.table.query.model;

/**
 * DelimitedIdentifier ::= {@link DoubleQuoteDelimitedIdentifier} | {@link BacktickDelimitedIdentifier}
 */
public class DelimitedIdentifier extends SimpleBranch {

	public DelimitedIdentifier(DoubleQuoteDelimitedIdentifier identifier) {
		super(identifier);
	}
	
	public DelimitedIdentifier(BacktickDelimitedIdentifier identifier) {
		super(identifier);
	}
	
	@Override
	public boolean hasQuotes(){
		return true;
	}

}
