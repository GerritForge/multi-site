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

package com.gerritforge.gerrit.plugins.multisite.forwarder.router;

import com.gerritforge.gerrit.plugins.multisite.MultiSiteMetrics;
import com.google.gerrit.metrics.Counter1;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class IndexEventMetrics extends MultiSiteMetrics {
  private final Counter1<String> terminalFailureCounter;

  @Inject
  public IndexEventMetrics(MetricMaker metricMaker) {
    terminalFailureCounter =
        metricMaker.newCounter(
            "multi_site/subscriber/subscriber_index_event_terminal_failure_counter",
            rateDescription("errors", "Number of index events ending in terminal failure"),
            stringField("event_type", "Index event type"));
  }

  public void incrementTerminalFailure(String eventType) {
    terminalFailureCounter.increment(eventType);
  }
}
