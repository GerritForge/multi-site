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

package com.gerritforge.gerrit.plugins.multisite.forwarder.events;

import com.gerritforge.gerrit.plugins.multisite.Configuration;

public enum EventTopic {
  INDEX_TOPIC("GERRIT.EVENT.INDEX", "indexEvent"),
  BATCH_INDEX_TOPIC("GERRIT.EVENT.BATCH.INDEX", "batchIndexEvent"),
  CACHE_TOPIC("GERRIT.EVENT.CACHE", "cacheEvent"),
  PROJECT_LIST_TOPIC("GERRIT.EVENT.PROJECT.LIST", "projectListEvent"),
  STREAM_EVENT_TOPIC("gerrit", "streamEvent");

  private final String topic;
  private final String aliasKey;

  private EventTopic(String topic, String aliasKey) {
    this.topic = topic;
    this.aliasKey = aliasKey;
  }

  public String topic(Configuration config) {
    return config.broker().getTopic(topicAliasKey(), topic);
  }

  public String topicAliasKey() {
    return aliasKey + "Topic";
  }

  public static EventTopic of(String topicString) {
    EventTopic[] topics = EventTopic.values();
    for (EventTopic topic : topics) {
      if (topic.topic.equals(topicString)) {
        return topic;
      }
    }
    return null;
  }
}
