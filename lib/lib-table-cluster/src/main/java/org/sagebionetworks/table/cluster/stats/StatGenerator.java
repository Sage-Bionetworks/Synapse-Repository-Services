package org.sagebionetworks.table.cluster.stats;

import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.SimpleBranch;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.TermPrime;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;

public class StatGenerator implements StatGeneratorInteface<Element> {

	@Override
	public Optional<ElementStats> generate(Element element, TableAndColumnMapper tableAndColumnMapper) {		
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
		
		if (element instanceof Term) {
			return generate(((Term) element).getFactor(), tableAndColumnMapper);
		}
		
		if (element instanceof TermPrime) {
			return new TermPrimeGenerator().generate((TermPrime) element, tableAndColumnMapper);
		}
		
		if (element instanceof Factor) {
			return generate(((Factor) element).getNumericPrimary(), tableAndColumnMapper);
		}
		
		if (element instanceof SimpleBranch) {
			return generate(((SimpleBranch) element).getChild(), tableAndColumnMapper);
		}
		
		return Optional.of(ElementStats.builder()
				.setMaximumSize(ColumnConstants.DEFAULT_STRING_SIZE)
				.build());
	}

}
