package org.sagebionetworks.table.cluster.stats;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.ArithmeticOperator;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.NumericPrimary;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RegularIdentifier;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.TermPrime;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class TermPrimeGeneratorTest {

	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private TermPrimeGenerator mockTermPrimeGenerator;
	
	private ColumnModel columnFoo;
	private List<ColumnModel> schema;
	private Map<String, ColumnModel> columnNameMap;
	
	
	@BeforeEach
	public void before() throws Exception {
		columnFoo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING);
		schema = Lists.newArrayList(columnFoo);
		columnNameMap = schema.stream()
			      .collect(Collectors.toMap(ColumnModel::getName, Function.identity()));
	}
	
	@Test
	public void testGenerateWithPlusSign() throws ParseException {
		QueryExpression rootModel = new TableQueryParser("SELECT foo FROM syn123").queryExpression();
		QuerySpecification model = rootModel.getFirstElementOfType(QuerySpecification.class);
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnNameMap.get("foo")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		
		TermPrime element = new TermPrime(ArithmeticOperator.PLUS_SIGN, new Term(new Factor(null, new NumericPrimary(new ValueExpressionPrimary(new ColumnReference(
				new ColumnName(new Identifier(new ActualIdentifier(new RegularIdentifier("foo")))), null))))));
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
	            .setMaximumSize(50L)
	            .build());
		
		assertEquals(expected, mockTermPrimeGenerator.generate(element, mapper));
	}
	
	@Test
	public void testGenerateWithUnimplementedOperator() throws ParseException {
		QueryExpression rootModel = new TableQueryParser("SELECT foo FROM syn123").queryExpression();
		QuerySpecification model = rootModel.getFirstElementOfType(QuerySpecification.class);
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnNameMap.get("foo")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		
		TermPrime element = new TermPrime(ArithmeticOperator.SOLIDUS, new Term(new Factor(null, new NumericPrimary(new ValueExpressionPrimary(new ColumnReference(
				new ColumnName(new Identifier(new ActualIdentifier(new RegularIdentifier("foo")))), null))))));
		
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, mockTermPrimeGenerator.generate(element, mapper));
	}
}
