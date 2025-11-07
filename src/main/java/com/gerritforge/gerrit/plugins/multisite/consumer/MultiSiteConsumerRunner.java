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

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gerritforge.gerrit.plugins.multisite.Configuration;

@Singleton
public class MultiSiteConsumerRunner implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final DynamicSet<AbstractSubcriber> consumers;
  private DynamicItem<BrokerApi> brokerApi;
  private Configuration cfg;

  @Inject
  public MultiSiteConsumerRunner(
      DynamicItem<BrokerApi> brokerApi,
      DynamicSet<AbstractSubcriber> consumers,
      Configuration cfg) {
    this.consumers = consumers;
    this.brokerApi = brokerApi;
    this.cfg = cfg;
  }

  @Override
  public void start() {
    logger.atInfo().log("starting consumers");
    consumers.forEach(
        consumer ->
            brokerApi.get().receiveAsync(consumer.getTopic().topic(cfg), consumer.getConsumer()));
  }

  @Override
  public void stop() {}
}
