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

package com.gerritforge.gerrit.plugins.multisite.cache;

import static org.mockito.Mockito.verifyNoInteractions;

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.gerritforge.gerrit.plugins.multisite.forwarder.CacheKeyJsonParser;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.gerrit.extensions.registration.DynamicSet;
import java.util.concurrent.Executor;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheEvictionHandlerTest {

  @Mock private Executor executorMock;
  private CachePatternMatcher defaultCacheMatcher =
      new CachePatternMatcher(new Configuration(new Config(), new Config()));

  @Mock private CacheKeyJsonParser cacheKeyJsonParser;

  @Test
  public void shouldNotPublishAccountsCacheEvictions() {
    String instanceId = "instance-id";
    final CacheEvictionHandler<String, String> handler =
        new CacheEvictionHandler<>(
            DynamicSet.emptySet(), executorMock, defaultCacheMatcher, instanceId, cacheKeyJsonParser);

    handler.onRemoval(
        "test", "accounts", RemovalNotification.create("test", "accounts", RemovalCause.EXPLICIT));

    verifyNoInteractions(executorMock);
  }
}
