package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

@ExtendWith(MockitoExtension.class)
public class AnnotationWriterImplTest {

	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private NodeManager mockNodeManager;
	@Mock
	private UserInfo mockUser;

	@InjectMocks
	private AnnotationWriterImpl writer;

	private String entityId;

	@BeforeEach
	public void before() {
		entityId = "syn123";
	}

	@Test
	public void testUpdateChangedAnnotations() {

		Map<String, AnnotationsValue> changed = new HashMap<>();
		changed.put("a", new AnnotationsValue().setType(AnnotationsValueType.BOOLEAN).setValue(List.of("true")));
		changed.put("b", null);

		Map<String, AnnotationsValue> current = new HashMap<>();
		current.put("a", new AnnotationsValue().setType(AnnotationsValueType.BOOLEAN).setValue(List.of("false")));
		current.put("b", new AnnotationsValue().setType(AnnotationsValueType.DOUBLE).setValue(List.of("3.4")));
		current.put("c", new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("one")));

		when(mockNodeDao.lockNode(entityId)).thenReturn("etag1");
		when(mockNodeManager.getUserAnnotations(mockUser, entityId))
				.thenReturn(new Annotations().setAnnotations(current).setEtag("etag1").setId(entityId));

		// call under test
		writer.updateChangedAnnotations(mockUser, "syn123", changed);

		Map<String, AnnotationsValue> merged = new HashMap<>();
		merged.put("a", new AnnotationsValue().setType(AnnotationsValueType.BOOLEAN).setValue(List.of("true")));
		merged.put("c", new AnnotationsValue().setType(AnnotationsValueType.STRING).setValue(List.of("one")));

		verify(mockNodeManager).updateUserAnnotations(mockUser, entityId,
				new Annotations().setEtag("etag1").setId(entityId).setAnnotations(merged));
	}

}
