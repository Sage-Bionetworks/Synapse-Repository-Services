package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.table.cluster.view.filter.IdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

@ExtendWith(MockitoExtension.class)
public class DatasetMetadataIndexProviderTest {

	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private NodeManager mockNodeManager;
	@InjectMocks
	private DatasetMetadataIndexProvider provider;

	private Long viewId;
	private List<EntityRef> items;
	private Set<IdAndVersion> scope;
	private Set<SubType> subTypes;
	private ViewScopeType viewScopeType;

	@BeforeEach
	public void before() {
		viewId = 123L;
		items = Arrays.asList(new EntityRef().setEntityId("syn11").setVersionNumber(2L),
				new EntityRef().setEntityId("syn22").setVersionNumber(3L));

		scope = items.stream().map(i -> IdAndVersion.newBuilder().setId(KeyFactory.stringToKey(i.getEntityId()))
				.setVersion(i.getVersionNumber()).build()).collect(Collectors.toSet());
		subTypes = Set.of(SubType.file);

		viewScopeType = new ViewScopeType(ViewObjectType.DATASET, 0L);
	}

	@Test
	public void testGetObjectType() {
		assertEquals(ViewObjectType.DATASET, provider.getObjectType());
	}

	@Test
	public void testGetColumnTypes() {
		assertEquals(ColumnType.ENTITYID, provider.getIdColumnType());
		assertEquals(ColumnType.ENTITYID, provider.getParentIdColumnType());
		assertEquals(ColumnType.ENTITYID, provider.getBenefactorIdColumnType());
	}

	@Test
	public void testGetViewFilter() {
		when(mockNodeDao.getNodeItems(viewId)).thenReturn(items);
		// call under test
		ViewFilter filter = provider.getViewFilter(viewId);
		ViewFilter expected = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		assertEquals(filter, expected);
	}

	@Test
	public void testGetViewFilterWithTypeAndScope() {
		// call under test
		ViewFilter filter = provider.getViewFilter(viewScopeType.getTypeMask(), scope);
		ViewFilter expected = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		assertEquals(filter, expected);
	}

	@Test
	public void testValidateScopeAndTypeWithUnderLimit() {
		Long typeMask = 0L;
		Set<Long> scopeIds = Set.of(1L, 2L);
		int maxContainersPerView = 3;
		// call under test
		provider.validateScopeAndType(typeMask, scopeIds, maxContainersPerView);
	}
	
	@Test
	public void testValidateScopeAndTypeWithOverLimit() {
		Long typeMask = 0L;
		Set<Long> scopeIds = Set.of(1L, 2L);
		int maxContainersPerView = 1;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			provider.validateScopeAndType(typeMask, scopeIds, maxContainersPerView);
		}).getMessage();
		assertEquals("Maximum of 1 items in a dataset exceeded.", message);
	}
	
	/**
	 * Test for PLFM
	 */
	@Test
	public void testGetDefaultColumnModel() {
		Long viewTypeMask = null;
		DefaultColumnModel expected = DefaultColumnModel.builder(ViewObjectType.DATASET)
				.withObjectField(Constants.BASIC_DEAFULT_COLUMNS)
				.withObjectField(Constants.FILE_SPECIFIC_COLUMNS).build();
		// call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(viewTypeMask);
		assertEquals(expected, model);
	}
}
