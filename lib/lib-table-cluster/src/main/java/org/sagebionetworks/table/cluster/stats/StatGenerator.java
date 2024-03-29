package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.ArrayFunctionSpecification;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SimpleBranch;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;
import org.sagebionetworks.util.ValidateArgument;

public class StatGenerator implements StatGeneratorInteface<Element> {

	@Override
	public Optional<ElementStats> generate(Element element, TableAndColumnMapper tableAndColumnMapper) {	
		ValidateArgument.required(tableAndColumnMapper, "tableAndColumnMapper");
		
		if (element == null) {
			return Optional.empty();
		}
		
		if (element instanceof ColumnReference) {
			return new ColumnReferenceGenerator().generate((ColumnReference) element, tableAndColumnMapper);
		}
		
		if (element instanceof UnsignedValueSpecification) {
			return new UnsignedValueSpecificationGenerator().generate((UnsignedValueSpecification) element, tableAndColumnMapper);
		}
		
		if (element instanceof MySqlFunction) {
			return new MySqlFunctionGenerator().generate((MySqlFunction) element, tableAndColumnMapper);
		}
		
		if (element instanceof SetFunctionSpecification) {
			return new SetFunctionSpecificationGenerator().generate((SetFunctionSpecification) element, tableAndColumnMapper);
		}
		
		if (element instanceof NumericValueExpression) {
			return new NumericValueExpressionGenerator().generate((NumericValueExpression) element, tableAndColumnMapper);
		}
		
		if (element instanceof ArrayFunctionSpecification) {
			return new ArrayFunctionSpecificationGenerator().generate((ArrayFunctionSpecification) element, tableAndColumnMapper);
		}
		
		if (element instanceof Term) {
			return generate(((Term) element).getFactor(), tableAndColumnMapper);
		}
		
		if (element instanceof Factor) {
			return generate(((Factor) element).getNumericPrimary(), tableAndColumnMapper);
		}
		
		// Catch all for SimpleBranches should be the last if statement as some of the implemented cases above may also be of SimpleBranch
		if (element instanceof SimpleBranch) {
			return generate(((SimpleBranch) element).getChild(), tableAndColumnMapper);
		}
		
		return Optional.empty();
	}

}
