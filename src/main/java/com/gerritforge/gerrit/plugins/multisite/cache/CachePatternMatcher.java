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

import com.gerritforge.gerrit.plugins.multisite.Configuration;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
class CachePatternMatcher {
  private static final ImmutableList<String> DEFAULT_PATTERNS =
      ImmutableList.of("^groups.*", "ldap_groups", "ldap_usernames", "projects", "sshkeys");

  private final Pattern pattern;

  @Inject
  CachePatternMatcher(Configuration cfg) {
    List<String> patterns = new ArrayList<>(DEFAULT_PATTERNS);
    patterns.addAll(cfg.cache().patterns());
    this.pattern = Pattern.compile(Joiner.on("|").join(patterns));
  }

  boolean matches(String cacheName) {
    return pattern.matcher(cacheName).matches();
  }
}
