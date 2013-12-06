package com.twitter.aurora.scheduler.storage.mem;

import com.google.common.base.Optional;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.twitter.aurora.scheduler.SchedulerLifecycle;
import com.twitter.aurora.scheduler.log.Log.Stream.StreamAccessException;
import com.twitter.aurora.scheduler.storage.Storage;
import com.twitter.aurora.scheduler.storage.Storage.MutateWork;
import com.twitter.aurora.scheduler.storage.Storage.Work.Quiet;
import com.twitter.aurora.scheduler.storage.mem.MemStorage.IllegalStorageStateException;
import com.twitter.common.testing.easymock.EasyMockTest;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import static junit.framework.Assert.fail;

/**
 * A class that tests the behaviour of MemStorage on log write time out errors.
 * These tests couldn't be part of MemStorageTest since we need the mocks provided by EasyMockTest.
 */
public class MemStorageShutdownTest extends EasyMockTest {

  private SchedulerLifecycle schedulerLifecycle;
  private MemStorage storage;
  private MutateWork<Void, Exception> writeWork;
  private Quiet<Void> readWork;

  @SuppressWarnings(value = "unchecked")
  @Before
  public void setUp() throws Exception {
    writeWork = createMock(MutateWork.class);
    readWork = createMock(Quiet.class);
    schedulerLifecycle = createMock(SchedulerLifecycle.class);
    storage = MemStorage.newEmptyStorage(Optional.of(schedulerLifecycle));
  }

  @Test(expected = IllegalStorageStateException.class)
  public void testLogUnAvailabilityOnWriteAfterWrite() throws Exception {
    expectDisabledStorageOnWrite();
    storage.write(writeWork);
  }

  @Test(expected = IllegalStorageStateException.class)
  public void testLogUnAvailabilityOnWriteAfterRead() throws Exception {
    expectDisabledStorageOnRead();
    storage.write(writeWork);
  }

  @Test(expected = IllegalStorageStateException.class)
  public void testLogUnAvailabilityOnReadAfterRead() throws Exception {
    expectDisabledStorageOnRead();
    storage.consistentRead(readWork);
  }

  @Test(expected = IllegalStorageStateException.class)
  public void testLogUnAvailabilityOnReadAfterWrite() throws Exception {
    expectDisabledStorageOnWrite();
    storage.consistentRead(readWork);
  }

  @Test(expected = IllegalStorageStateException.class)
  public void testLogUnavailabilityOnWeakReadAfterRead() throws Exception {
    expectDisabledStorageOnRead();
    storage.weaklyConsistentRead(readWork);
  }

  @Test(expected = IllegalStorageStateException.class)
  public void testLogUnavailabilityOnWeakReadAfterWrite() throws Exception {
    expectDisabledStorageOnWrite();
    storage.weaklyConsistentRead(readWork);
  }

  private void expectDisabledStorageOnWrite() throws Exception {
    setExpectations(false);
    try {
      storage.write(writeWork);
      fail("Storage failed to throw an exception on write");
    } catch (StreamAccessException e) {
      // Expected
    }
  }

  private void expectDisabledStorageOnRead() throws Exception {
    setExpectations(true);

    try {
      storage.consistentRead(readWork);
      fail("Storage failed to throw an exception on read");
    } catch (StreamAccessException e) {
      // Expected
    }
  }

  private void setExpectations(boolean isReadMode) throws Exception {
    final Capture<Storage.MutableStoreProvider> storeProvider = createCapture();
    final StreamAccessException timeoutException = new StreamAccessException("time out");
    if (isReadMode) {
      expect(readWork.apply(capture(storeProvider))).andThrow(timeoutException);
    } else {
      expect(writeWork.apply(capture(storeProvider))).andThrow(timeoutException);
    }

    schedulerLifecycle.shutdown();
    expectLastCall();

    control.replay();
  }
}
