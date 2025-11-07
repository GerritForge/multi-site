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

package com.gerritforge.gerrit.plugins.multisite.validation;

import com.gerritforge.gerrit.globalrefdb.RefDbLockException;
import com.gerritforge.gerrit.globalrefdb.validation.RefLocker;
import com.google.gerrit.entities.Project;
import com.google.inject.Inject;
import com.gerritforge.gerrit.plugins.multisite.Configuration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ReentrantRefDbLocker implements RefLocker {
  private final ConcurrentHashMap<String, RefLock> refsLocks;
  private final long timeoutMsec;

  private class RefLock implements AutoCloseable {
    private final Lock lock;

    RefLock() {
      lock = new ReentrantLock();
    }

    boolean lock() throws InterruptedException {
      return lock.tryLock(timeoutMsec, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
      lock.unlock();
    }
  }

  @Inject
  public ReentrantRefDbLocker(Configuration configuration) {
    this.timeoutMsec = configuration.localRefLockTimeoutMsec();
    refsLocks = new ConcurrentHashMap<>();
  }

  @Override
  public AutoCloseable lockRef(Project.NameKey project, String refName) throws RefDbLockException {
    RefLock lock = refsLocks.computeIfAbsent(getKey(project, refName), ref -> new RefLock());
    try {
      if (lock.lock()) {
        return lock;
      }

      throw new RefDbLockException(
          project.get(),
          refName,
          String.format("Unable to acquire local ref lock after %s msec", timeoutMsec));
    } catch (InterruptedException e) {
      throw new RefDbLockException(project.get(), refName, e);
    }
  }

  private String getKey(Project.NameKey project, String refName) {
    return String.format("%s:%s", project.get(), refName);
  }
}
