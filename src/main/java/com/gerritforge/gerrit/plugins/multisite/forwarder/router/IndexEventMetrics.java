// Copyright (C) 2026 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import com.gerritforge.gerrit.plugins.multisite.MultiSiteMetrics;
import com.google.gerrit.metrics.CallbackMetric1;
import com.google.gerrit.metrics.Counter1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class IndexEventMetrics extends MultiSiteMetrics {
  private final Counter1<String> terminalFailureCounter;
  private final Map<String, Long> unacknowledgedEvents = new ConcurrentHashMap<>();

  @Inject
  public IndexEventMetrics(MetricMaker metricMaker) {
    terminalFailureCounter =
        metricMaker.newCounter(
            "multi_site/subscriber/manual_ack/index_event_terminal_failure_counter",
            rateDescription("errors", "Number of index events ending in terminal failure"),
            stringField("event_type", "Index event type"));

    CallbackMetric1<String, Long> unacknowledgedEventsMetric =
        metricMaker.newCallbackMetric(
            "multi_site/subscriber/manual_ack/index_event_unacknowledged",
            Long.class,
            new Description("Number of unacknowledged index events").setGauge().setUnit("messages"),
            stringField("event_type", "Index event type"));
    metricMaker.newTrigger(
        unacknowledgedEventsMetric,
        () -> unacknowledgedEvents.forEach(unacknowledgedEventsMetric::set));
  }

  public void incrementManualAckTerminalFailure(String eventType) {
    terminalFailureCounter.increment(eventType);
  }

  public void incrementUnacknowledgedEvent(String eventType) {
    unacknowledgedEvents.merge(eventType, 1L, Long::sum);
  }

  public void resetUnacknowledgedEvents(String eventType) {
    unacknowledgedEvents.put(eventType, 0L);
  }
}
