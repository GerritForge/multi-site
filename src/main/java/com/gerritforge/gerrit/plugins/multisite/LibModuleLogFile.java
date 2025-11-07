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

package com.gerritforge.gerrit.plugins.multisite;

import com.google.gerrit.server.util.SystemLog;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public abstract class LibModuleLogFile {

  public LibModuleLogFile(SystemLog systemLog, String logName, Layout layout) {
    Logger logger = LogManager.getLogger(logName);
    if (logger.getAppender(logName) == null) {
      AsyncAppender asyncAppender = systemLog.createAsyncAppender(logName, layout, true, true);
      logger.addAppender(asyncAppender);
      logger.setAdditivity(false);
    }
  }
}
