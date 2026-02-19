// Copyright (C) 2025 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.multisite.consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.gerritforge.gerrit.eventbroker.MessageAcknowledgementException;
import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.gerritforge.gerrit.globalrefdb.validation.ProjectsFilter;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.Configuration.Broker;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheNotFoundException;
import com.gerritforge.gerrit.plugins.multisite.forwarder.router.ForwardedEventRouter;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.permissions.PermissionBackendException;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public abstract class AbstractSubscriberTestBase {
  protected static final String NODE_INSTANCE_ID = "node-instance-id";
  protected static final String INSTANCE_ID = "other-node-instance-id";
  protected static final String PROJECT_NAME = "project-name";

  @Mock protected DroppedEventListener droppedEventListeners;
  @Mock protected MessageLogger msgLog;
  @Mock protected SubscriberMetrics subscriberMetrics;
  @Mock protected Configuration cfg;
  @Mock protected Broker brokerCfg;
  @Mock protected ProjectsFilter projectsFilter;

  @SuppressWarnings("rawtypes")
  protected ForwardedEventRouter eventRouter;

  protected AbstractSubcriber objectUnderTest;

  @Before
  public void setup() {
    when(cfg.broker()).thenReturn(brokerCfg);
    when(brokerCfg.getTopic(any(), any())).thenReturn("test-topic");
    eventRouter = eventRouter();
    objectUnderTest = objectUnderTest();
  }

  @Test
  public void shouldConsumeEventsWhenNotFilteredByProjectName()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    for (Event event : events()) {
      when(projectsFilter.matches(any(String.class))).thenReturn(true);
      TestAck ack = new TestAck();
      objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);
      verifyConsumed(event, ack);
    }
  }

  @Test
  public void shouldSkipEventsWhenFilteredByProjectName()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    for (Event event : events()) {
      when(projectsFilter.matches(any(String.class))).thenReturn(false);
      TestAck ack = new TestAck();
      objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);
      verifySkipped(event, ack);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldSkipLocalEvents()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    for (Event event : events()) {
      event.instanceId = NODE_INSTANCE_ID;
      when(projectsFilter.matches(any(String.class))).thenReturn(true);

      TestAck ack = new TestAck();
      objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);

      verify(projectsFilter, never()).matches(PROJECT_NAME);
      verify(eventRouter, never()).route(event);
      verify(droppedEventListeners, times(1)).onEventDropped(event);
      ack.assertAckAttemptedOnce();
      reset(projectsFilter, eventRouter, droppedEventListeners);
    }
  }

  @Test
  public void shouldUpdateReplicationMetricsWithLocalEvents() {
    for (Event event : events()) {
      event.instanceId = NODE_INSTANCE_ID;
      when(projectsFilter.matches(any(String.class))).thenReturn(true);

      TestAck ack = new TestAck();
      objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);

      verify(subscriberMetrics, times(1)).updateReplicationStatusMetrics(event);
      reset(projectsFilter, eventRouter, droppedEventListeners, subscriberMetrics);
    }
  }

  @Test
  public void shouldUpdateReplicationMetricsWithNonLocalEvents() {
    for (Event event : events()) {
      event.instanceId = INSTANCE_ID;
      when(projectsFilter.matches(any(String.class))).thenReturn(true);

      TestAck ack = new TestAck();
      objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);

      verify(subscriberMetrics, times(1)).updateReplicationStatusMetrics(event);
      reset(projectsFilter, eventRouter, droppedEventListeners, subscriberMetrics);
    }
  }

  @Test
  public void shouldNotAckWhenMessageIsAutoAcked()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    Event event = events().getFirst();
    when(projectsFilter.matches(any(String.class))).thenReturn(true);
    TestAck ack = new TestAck(/* autoAck */ true);

    objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);

    verifyConsumed(event, ack);
    ack.assertNotAcked();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldNotCountConsumedMessageWhenAckFails()
      throws IOException, PermissionBackendException, CacheNotFoundException {
    Event event = events().getFirst();
    when(projectsFilter.matches(any(String.class))).thenReturn(true);
    TestAck ack = TestAck.failing();

    objectUnderTest.getConsumer(ack.isAutoAck()).accept(event, ack);

    verify(projectsFilter, times(1)).matches(PROJECT_NAME);
    verify(eventRouter, times(1)).route(event);
    ack.assertAckAttemptedOnce();
    verify(subscriberMetrics, times(1)).incrementSubscriberFailedToAckMessage();
    verify(subscriberMetrics, never()).incrementSubscriberConsumedMessage();
    reset(projectsFilter, eventRouter, droppedEventListeners, subscriberMetrics);
  }

  protected abstract AbstractSubcriber objectUnderTest();

  protected abstract List<Event> events();

  @SuppressWarnings("rawtypes")
  protected abstract ForwardedEventRouter eventRouter();

  @SuppressWarnings("unchecked")
  protected void verifySkipped(Event event, TestAck ack)
      throws IOException, PermissionBackendException, CacheNotFoundException {
    verify(projectsFilter, times(1)).matches(PROJECT_NAME);
    verify(eventRouter, never()).route(event);
    verify(droppedEventListeners, times(1)).onEventDropped(event);
    ack.assertAckAttemptedOnce();
    reset(projectsFilter, eventRouter, droppedEventListeners);
  }

  @SuppressWarnings("unchecked")
  protected void verifyConsumed(Event event, TestAck ack)
      throws IOException, PermissionBackendException, CacheNotFoundException {
    verify(projectsFilter, times(1)).matches(PROJECT_NAME);
    verify(eventRouter, times(1)).route(event);
    verify(droppedEventListeners, never()).onEventDropped(event);
    if (!ack.isAutoAck()) {
      ack.assertAckAttemptedOnce();
    }
    reset(projectsFilter, eventRouter, droppedEventListeners);
  }

  protected DynamicSet<DroppedEventListener> asDynamicSet(DroppedEventListener listener) {
    DynamicSet<DroppedEventListener> result = new DynamicSet<>();
    result.add("multi-site", listener);
    return result;
  }

  protected static class TestAck implements MessageAcknowledgement<Event> {
    private final boolean autoAck;
    private final boolean fail;
    private int ackCount;

    TestAck() {
      this(/* autoAck */ false, /* fail */ false);
    }

    TestAck(boolean autoAck) {
      this(autoAck, /* fail */ false);
    }

    TestAck(boolean autoAck, boolean fail) {
      this.autoAck = autoAck;
      this.fail = fail;
    }

    private static TestAck failing() {
      return new TestAck(/* autoAck */ false, /* fail */ true);
    }

    @Override
    public void ack(Event event) {
      ackCount++;
      if (fail) {
        throw new MessageAcknowledgementException("ack failed");
      }
    }

    public boolean isAutoAck() {
      return autoAck;
    }

    private void assertAckAttemptedOnce() {
      assertEquals(1, ackCount);
    }

    private void assertNotAcked() {
      assertEquals(0, ackCount);
    }
  }
}
