package com.twitter.aurora.scheduler.log.mesos;

import java.util.concurrent.TimeoutException;

import javax.inject.Provider;

import com.google.inject.util.Providers;

import org.apache.mesos.Log;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.twitter.aurora.scheduler.log.Log.Stream.StreamAccessException;
import com.twitter.aurora.scheduler.log.mesos.LogInterface.ReaderInterface;
import com.twitter.aurora.scheduler.log.mesos.LogInterface.WriterInterface;
import com.twitter.common.quantity.Amount;
import com.twitter.common.quantity.Time;
import com.twitter.common.testing.easymock.EasyMockTest;

import static org.easymock.EasyMock.expect;

public class MesosLogTest extends EasyMockTest {

  private static final Amount<Long, Time> READ_TIMEOUT = Amount.of(5L, Time.SECONDS);
  private static final Amount<Long, Time> WRITE_TIMEOUT = Amount.of(3L, Time.SECONDS);
  private static final byte[] DUMMY_CONTENT = "test data".getBytes();

  private MesosLog.LogStream logStream;
  private MesosLog.LogStream.Mutation<String> dummyMutation;
  private MesosLog.LogStream.OpStats stats;

  @Before
  public void setUp() {
    LogInterface logInterface = createMock(LogInterface.class);
    ReaderInterface reader = createMock(ReaderInterface.class);
    Provider<WriterInterface> writerFactory = Providers.of(createMock(WriterInterface.class));

    dummyMutation = createMock(new Clazz<MesosLog.LogStream.Mutation<String>>() { });
    stats = new MesosLog.LogStream.OpStats("test");
    logStream = new MesosLog.LogStream(logInterface, reader, READ_TIMEOUT,
        writerFactory, WRITE_TIMEOUT, DUMMY_CONTENT);
  }

  @Test
  public void testLogStreamTimeout() throws TimeoutException, Log.WriterFailedException {
    expect(dummyMutation.apply(EasyMock.<WriterInterface>anyObject()))
        .andThrow(new TimeoutException("Task timed out"));

    control.replay();

    try {
      logStream.mutate(stats, dummyMutation);
    } catch (StreamAccessException e) {
      // Expected;
    }

    try {
      logStream.mutate(stats, dummyMutation);
    } catch (StreamAccessException e) {
      // Expected since log is disabled on a write time out
    }
  }

  @Test
  public void testLogStreamWriteFailure() throws TimeoutException, Log.WriterFailedException {
    expect(dummyMutation.apply(EasyMock.<WriterInterface>anyObject()))
        .andThrow(new Log.WriterFailedException("Failed to write to log")).times(1);

    expect(dummyMutation.apply(EasyMock.<WriterInterface>anyObject()))
        .andReturn(String.valueOf(DUMMY_CONTENT)).times(1);

    control.replay();

    try {
      logStream.mutate(stats, dummyMutation);
    } catch (StreamAccessException e) {
      // Expected
    }

    logStream.mutate(stats, dummyMutation); // no error, since log is not disabled on write failure
  }
}
