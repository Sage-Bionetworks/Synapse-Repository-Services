package org.sagebionetworks.table.cluster.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.ArithmeticOperator;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.GeneralLiteral;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.MySqlFunctionName;
import org.sagebionetworks.table.query.model.NumericPrimary;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RegularIdentifier;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SimpleBranch;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.TermPrime;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.util.SqlElementUtils;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class StatGeneratorTest {

	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@Mock
	private UnsignedValueSpecificationGenerator unsignedValueSpecificationGenerator;
	
	@InjectMocks
	private StatGenerator statGeneratorSpy;
	
	
	private ColumnModel columnFoo;
	private List<ColumnModel> schema;
	private Map<String, ColumnModel> columnNameMap;
	
	private Term fooTerm = new Term(new Factor(null, new NumericPrimary(new ValueExpressionPrimary(new ColumnReference(
			new ColumnName(new Identifier(new ActualIdentifier(new RegularIdentifier("foo")))), null)))));
	
	
	@BeforeEach
	public void before() throws Exception {
		columnFoo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING);
		schema = Lists.newArrayList(columnFoo);
		
		columnNameMap = schema.stream()
			      .collect(Collectors.toMap(ColumnModel::getName, Function.identity()));
		
		statGeneratorSpy = spy(new StatGenerator());
	}
	
	@Test
	public void testGenerateWithNull() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = null;
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithColumnReference() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = SqlElementUtils.createColumnReference("foo");
		Optional<ElementStats> expected = Optional.of(ElementStats.builder().setMaximumSize(50L).build());
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithUnsignedValueSpecification() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new UnsignedValueSpecification(new UnsignedLiteral(
				new GeneralLiteral(new CharacterStringLiteral("123"))));
		Optional<ElementStats> expected = Optional.of(ElementStats.builder().setMaximumSize(3L).build());
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithMySqlFunction() throws ParseException {
		setupTableAndColumnMapper();
		
		MySqlFunction element = new MySqlFunction(MySqlFunctionName.CONCAT);
		element.setParameterValues(SqlElementUtils.createValueExpressions("foo", "'12345'"));
		Optional<ElementStats> expected = Optional.of(ElementStats.builder().setMaximumSize(55L).build());
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithSetFunctionSpecification() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new SetFunctionSpecification(true);
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithNumericValueExpression() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new NumericValueExpression(fooTerm);
		Optional<ElementStats> expected = Optional.of(ElementStats.builder().setMaximumSize(50L).build());
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithTerm() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = fooTerm;

		statGeneratorSpy.generate(element, mockTableAndColumnMapper);
		
		verify(statGeneratorSpy).generate(element, mockTableAndColumnMapper);
		verify(statGeneratorSpy).generate(((Term) element).getFactor(), mockTableAndColumnMapper);
	}
	
	@Test
	public void testGenerateWithTermPrime() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new TermPrime(ArithmeticOperator.PLUS_SIGN, fooTerm);
		Optional<ElementStats> expected = Optional.of(ElementStats.builder().setMaximumSize(50L).build());
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithFactor() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new Factor(null, new NumericPrimary(new ValueExpressionPrimary(new ColumnReference(
				new ColumnName(new Identifier(new ActualIdentifier(new RegularIdentifier("foo")))), null))));

		statGeneratorSpy.generate(element, mockTableAndColumnMapper);
		
		verify(statGeneratorSpy).generate(element, mockTableAndColumnMapper);
		verify(statGeneratorSpy).generate(((Factor) element).getNumericPrimary(), mockTableAndColumnMapper);
	}
	
	@Test
	public void testGenerateWithSimpleBranch() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new ActualIdentifier(new RegularIdentifier("string"));

		statGeneratorSpy.generate(element, mockTableAndColumnMapper);
		
		verify(statGeneratorSpy).generate(element, mockTableAndColumnMapper);
		verify(statGeneratorSpy).generate(((SimpleBranch) element).getChild(), mockTableAndColumnMapper);
	}
	
	@Test
	public void testGenerateWithUnspecifiedCase() throws ParseException {
		setupTableAndColumnMapper();
		
		Element element = new RegularIdentifier("string");
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(ColumnConstants.DEFAULT_STRING_SIZE)
				.build());
		
		assertEquals(expected, statGeneratorSpy.generate(element, mockTableAndColumnMapper));
	}
	
	public void setupTableAndColumnMapper() throws ParseException {
		QueryExpression rootModel = new TableQueryParser("SELECT foo FROM syn123").queryExpression();
		QuerySpecification model = rootModel.getFirstElementOfType(QuerySpecification.class);
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnNameMap.get("foo")));
		
		mockTableAndColumnMapper = new TableAndColumnMapper(model, mockSchemaProvider);
	}
	
}
