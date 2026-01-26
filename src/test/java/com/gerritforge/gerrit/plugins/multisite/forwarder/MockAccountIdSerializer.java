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

import com.google.gerrit.entities.Account;
import com.google.gerrit.server.cache.serialize.CacheSerializer;
import org.junit.Ignore;

import java.nio.charset.StandardCharsets;

@Ignore
public class MockAccountIdSerializer implements CacheSerializer<Account.Id>{

  @Override
  public byte[] serialize(Account.Id accountId) {
    return String.valueOf(accountId.id()).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Account.Id deserialize(byte[] in) {
    return Account.Id.tryParse(new String(in, StandardCharsets.UTF_8)).get();
  }
}

