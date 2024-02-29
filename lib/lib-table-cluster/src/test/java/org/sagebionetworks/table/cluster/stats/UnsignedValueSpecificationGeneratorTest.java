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
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.GeneralLiteral;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class UnsignedValueSpecificationGeneratorTest {

	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private UnsignedValueSpecificationGenerator generator;
	
	
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
	public void testGenerate() throws ParseException {
		QueryExpression rootModel = new TableQueryParser("SELECT foo FROM syn123").queryExpression();
		QuerySpecification model = rootModel.getFirstElementOfType(QuerySpecification.class);
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnNameMap.get("foo")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		
		UnsignedValueSpecification element = new UnsignedValueSpecification(
				new UnsignedLiteral(new GeneralLiteral(new CharacterStringLiteral("12345"))));
		
		Optional<ElementsStats> expected = Optional.of(ElementsStats.builder()
	            .setMaximumSize(5L)
	            .build());
		
		assertEquals(expected, generator.generate(element, mapper));
	}
}
