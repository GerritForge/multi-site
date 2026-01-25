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

package com.gerritforge.gerrit.plugins.multisite.forwarder;

import com.google.gerrit.server.cache.serialize.CacheSerializer;
import com.google.gerrit.server.change.ChangeKindCacheImpl;
import java.nio.charset.StandardCharsets;
import org.eclipse.jgit.lib.ObjectId;

public class MockMultiSiteChangeKindCacheKeySerializer
    implements CacheSerializer<ChangeKindCacheImpl.Key> {

  private static final String SEPARATOR = "@";

  @Override
  public byte[] serialize(ChangeKindCacheImpl.Key object) {
    return String.join(SEPARATOR, object.prior().name(), object.next().name(), object.strategyName())
        .getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public ChangeKindCacheImpl.Key deserialize(byte[] in) {
    String[] parts = new String(in, StandardCharsets.UTF_8).split(SEPARATOR);
    return ChangeKindCacheImpl.Key.create(
        ObjectId.fromString(parts[0]), ObjectId.fromString(parts[1]), parts[2]);
  }
}
