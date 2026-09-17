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

package com.gerritforge.gerrit.plugins.multisite.broker;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.plugins.multisite.forwarder.events.AccountIndexEvent;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokerApiWrapperTest {
  private static final String DEFAULT_INSTANCE_ID = "instance-id";
  @Mock private BrokerMetrics brokerMetrics;
  @Mock private BrokerApi brokerApi;
  @Mock Event event;
  private String topic = "index";

  private BrokerApiWrapper objectUnderTest;

  @Before
  public void setUp() {
    event.instanceId = DEFAULT_INSTANCE_ID;
    objectUnderTest =
        new BrokerApiWrapper(
            MoreExecutors.directExecutor(),
            DynamicItem.itemOf(BrokerApi.class, brokerApi),
            brokerMetrics,
            DEFAULT_INSTANCE_ID);
  }

  @Test
  public void shouldIncrementBrokerMetricCounterWhenMessagePublished() {
    brokerReturns(true);
    objectUnderTest.send(topic, event);
    verify(brokerMetrics, only()).incrementBrokerPublishedMessage();
  }

  @Test
  public void shouldPublishMessage() {
    brokerReturns(true);

    objectUnderTest.send(topic, event);

    verify(brokerApi).send(topic, event);
  }

  @Test
  public void shouldIncrementFailureMetricWhenPublishingReturnsFalse() {
    brokerReturns(false);

    objectUnderTest.send(topic, event);

    verify(brokerMetrics, only()).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldRequeueMessageFromAnotherInstance() {
    brokerReturns(true);
    AccountIndexEvent multiSiteEvent = new AccountIndexEvent(1, null, "other-instance-id", false);

    objectUnderTest.requeue(topic, multiSiteEvent);

    Event sentEvent = captureRequeueEvent();
    assertThat(sentEvent).isInstanceOf(AccountIndexEvent.class);
    assertThat(sentEvent).isNotSameInstanceAs(multiSiteEvent);
    verify(brokerMetrics).incrementBrokerRequeuedMessage(topic, AccountIndexEvent.TYPE);
  }

  @Test
  public void shouldMarkMultiSiteEventCopyAsRequeuedBeforePublishing() {
    brokerReturns(true);
    AccountIndexEvent multiSiteEvent = new AccountIndexEvent(1, null, "other-instance-id", false);
    long beforeRequeue = System.currentTimeMillis() / 1000;

    objectUnderTest.requeue(topic, multiSiteEvent);

    AccountIndexEvent sentEvent = (AccountIndexEvent) captureRequeueEvent();
    assertThat(multiSiteEvent.isRequeued()).isFalse();
    assertThat(sentEvent.isRequeued()).isTrue();
    assertThat(sentEvent.getRetryCount()).isEqualTo(1);
    assertThat(sentEvent.getRequeuedOn()).isAtLeast(beforeRequeue);
    assertThat(sentEvent.getRequeuedByInstanceId()).isEqualTo(DEFAULT_INSTANCE_ID);
  }

  @Test
  public void shouldIncrementMultiSiteEventRetryCountOnRequeuedCopy() {
    brokerReturns(true);
    AccountIndexEvent multiSiteEvent = new AccountIndexEvent(1, null, "other-instance-id", false);
    multiSiteEvent.markRequeued("previous-instance-id");

    objectUnderTest.requeue(topic, multiSiteEvent);

    AccountIndexEvent sentEvent = (AccountIndexEvent) captureRequeueEvent();
    assertThat(multiSiteEvent.getRetryCount()).isEqualTo(1);
    assertThat(sentEvent.getRetryCount()).isEqualTo(2);
  }

  @Test
  public void shouldIncrementFailedRequeueMetricWhenBrokerReturnsFalse() {
    brokerReturns(false);
    AccountIndexEvent multiSiteEvent = new AccountIndexEvent(1, null, "other-instance-id", false);

    objectUnderTest.requeue(topic, multiSiteEvent);

    verify(brokerMetrics, only())
        .incrementBrokerFailedToRequeueMessage(topic, AccountIndexEvent.TYPE);
  }

  @Test
  public void shouldIncrementFailedRequeueMetricWhenBrokerFails() {
    brokerFails(new Exception("Force Future failure"));
    AccountIndexEvent multiSiteEvent = new AccountIndexEvent(1, null, "other-instance-id", false);

    objectUnderTest.requeue(topic, multiSiteEvent);

    verify(brokerMetrics, only())
        .incrementBrokerFailedToRequeueMessage(topic, AccountIndexEvent.TYPE);
  }

  @Test
  public void shouldIncrementFailedRequeueMetricWhenBrokerThrows() {
    AccountIndexEvent multiSiteEvent = new AccountIndexEvent(1, null, "other-instance-id", false);
    when(brokerApi.requeue(any(), any())).thenThrow(new RuntimeException("Unexpected exception"));

    assertThrows(RuntimeException.class, () -> objectUnderTest.requeue(topic, multiSiteEvent));

    verify(brokerMetrics, only())
        .incrementBrokerFailedToRequeueMessage(topic, AccountIndexEvent.TYPE);
  }

  @Test
  public void shouldIncrementBrokerFailedMetricCounterWhenMessagePublishingFailed() {
    brokerFails(new Exception("Force Future failure"));
    objectUnderTest.send(topic, event);
    verify(brokerMetrics, only()).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedMetricCounterWhenUnexpectedException() {
    when(brokerApi.send(any(), any()))
        .thenThrow(new RuntimeException("Unexpected runtime exception"));
    try {
      objectUnderTest.sendSync(topic, event);
    } catch (RuntimeException e) {
      // expected
    }
    verify(brokerMetrics, only()).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldSkipMessageSendingWhenInstanceIdIsNull() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = null;
    objectUnderTest.send(topic, event);
    verify(brokerApi, never()).send(any(), eq(event));
  }

  @Test
  public void shouldSkipMessageSendingWhenInstanceIdIsEmpty() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "";
    objectUnderTest.send(topic, event);
    verify(brokerApi, never()).send(any(), eq(event));
  }

  private void brokerReturns(boolean result) {
    lenient().when(brokerApi.send(any(), any())).thenReturn(Futures.immediateFuture(result));
    lenient().when(brokerApi.requeue(any(), any())).thenReturn(Futures.immediateFuture(result));
  }

  private void brokerFails(Throwable failure) {
    lenient().when(brokerApi.send(any(), any())).thenReturn(Futures.immediateFailedFuture(failure));
    lenient()
        .when(brokerApi.requeue(any(), any()))
        .thenReturn(Futures.immediateFailedFuture(failure));
  }

  private Event captureSentEvent() {
    ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
    verify(brokerApi).send(eq(topic), sentEvent.capture());
    return sentEvent.getValue();
  }

  private Event captureRequeueEvent() {
    ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
    verify(brokerApi).requeue(eq(topic), sentEvent.capture());
    return sentEvent.getValue();
  }
}
