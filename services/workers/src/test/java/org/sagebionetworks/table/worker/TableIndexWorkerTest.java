package org.sagebionetworks.table.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@RunWith(MockitoJUnitRunner.class)
public class TableIndexWorkerTest {

	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	TableEntityManager mockTableEntityManager;
	@Mock
	TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexManager mockTableIndexManger;
	@Mock
	ProgressCallback mockProgressCallback;

	@InjectMocks
	TableIndexWorker worker;

	IdAndVersion idAndVersion;

	ChangeMessage message;

	@Before
	public void before() {
		idAndVersion = IdAndVersion.parse("syn213");
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockTableIndexManger);
		message = new ChangeMessage();
		message.setObjectType(ObjectType.TABLE);
		message.setObjectId(idAndVersion.toString());
		message.setChangeType(ChangeType.CREATE);
	}

	@Test
	public void testRunNonTable() throws RecoverableMessageException, Exception {
		message.setObjectType(ObjectType.FILE);
		// call under test
		worker.run(mockProgressCallback, message);
		verifyZeroInteractions(mockConnectionFactory);
		verifyZeroInteractions(mockTableEntityManager);
	}

	/**
	 * Map TableIndexConnectionUnavailableException to RecoverableMessageException.
	 * 
	 * @throws Exception
	 * @throws RecoverableMessageException
	 */
	@Test(expected = RecoverableMessageException.class)
	public void testRunTableIndexConnectionUnavailableException() throws RecoverableMessageException, Exception {
		TableIndexConnectionUnavailableException exception = new TableIndexConnectionUnavailableException("not now");
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, message);
	}

	@Test
	public void testRunDeleteMessage() throws Exception {
		message.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(mockProgressCallback, message);
		verify(mockTableEntityManager).deleteTableIfDoesNotExist(message.getObjectId());
		verify(mockTableIndexManger).deleteTableIndex(idAndVersion);
		verify(mockTableManagerSupport, never()).getLastTableChangeNumber(any(IdAndVersion.class));
	}

	@Test
	public void testRunCreateMessage() throws RecoverableMessageException, Exception {
		long targetChangeNumber = 12L;
		when(mockTableManagerSupport.getLastTableChangeNumber(idAndVersion))
				.thenReturn(Optional.of(targetChangeNumber));
		Iterator<TableChangeMetaData> iterator = new LinkedList<TableChangeMetaData>().iterator();
		when(mockTableEntityManager.newTableChangeIterator(message.getObjectId())).thenReturn(iterator);
		// call under test
		worker.run(mockProgressCallback, message);
		verify(mockTableEntityManager, never()).deleteTableIfDoesNotExist(anyString());
		verify(mockTableIndexManger, never()).deleteTableIndex(any(IdAndVersion.class));
		verify(mockTableIndexManger).buildIndexToChangeNumber(mockProgressCallback, idAndVersion, iterator,
				targetChangeNumber);
	}

	@Test
	public void testRunCreateMessageNoTarget() throws RecoverableMessageException, Exception {
		// empty means no change for this table.
		when(mockTableManagerSupport.getLastTableChangeNumber(idAndVersion)).thenReturn(Optional.empty());
		Iterator<TableChangeMetaData> iterator = new LinkedList<TableChangeMetaData>().iterator();
		when(mockTableEntityManager.newTableChangeIterator(message.getObjectId())).thenReturn(iterator);
		// call under test
		worker.run(mockProgressCallback, message);
		verify(mockTableIndexManger, never()).buildIndexToChangeNumber(any(ProgressCallback.class), any(IdAndVersion.class),
				any(Iterator.class), anyLong());
	}
}
