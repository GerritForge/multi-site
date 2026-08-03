// Copyright (C) 2026 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://mariadb.com/bsl11
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.plugins.multisite.forwarder.events.ChangeIndexEvent;
import com.google.gerrit.metrics.CallbackMetric1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.Field;
import com.google.gerrit.metrics.MetricMaker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexEventMetricsTest {
  @Mock private MetricMaker metricMaker;
  @Mock private CallbackMetric1<String, Long> eventsPendingAcknowledgementMetric;
  private IndexEventMetrics metrics;
  private Runnable trigger;

  @Before
  public void setUp() {
    when(metricMaker.newCallbackMetric(
            anyString(), eq(Long.class), any(Description.class), any(Field.class)))
        .thenReturn(eventsPendingAcknowledgementMetric);
    metrics = new IndexEventMetrics(metricMaker);
    ArgumentCaptor<Runnable> triggerCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(metricMaker).newTrigger(eq(eventsPendingAcknowledgementMetric), triggerCaptor.capture());
    trigger = triggerCaptor.getValue();
  }

  @Test
  public void shouldReportNumberOfEventsPendingAcknowledgement() {
    metrics.incrementEventsPendingAcknowledgement(ChangeIndexEvent.TYPE);
    metrics.incrementEventsPendingAcknowledgement(ChangeIndexEvent.TYPE);

    trigger.run();

    verify(eventsPendingAcknowledgementMetric).set(ChangeIndexEvent.TYPE, 2L);
  }

  @Test
  public void shouldReportZeroAfterReset() {
    metrics.incrementEventsPendingAcknowledgement(ChangeIndexEvent.TYPE);
    metrics.resetEventsPendingAcknowledgement(ChangeIndexEvent.TYPE);

    trigger.run();

    verify(eventsPendingAcknowledgementMetric).set(ChangeIndexEvent.TYPE, 0L);
  }
}
