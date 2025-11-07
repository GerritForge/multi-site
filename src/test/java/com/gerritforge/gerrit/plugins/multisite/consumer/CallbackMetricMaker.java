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

import com.google.gerrit.metrics.Counter1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.Field;
import org.junit.Ignore;

@Ignore
public class CallbackMetricMaker extends DisabledMetricMaker {
  private int callbackMetricCounter = 0;

  public int getCallbackMetricCounter() {
    return callbackMetricCounter;
  }

  @Override
  public <F1> Counter1<F1> newCounter(String name, Description desc, Field<F1> field1) {
    callbackMetricCounter += 1;
    return super.newCounter(name, desc, field1);
  }

  public void resetCallbackMetricCounter() {
    callbackMetricCounter = 0;
  }
}
