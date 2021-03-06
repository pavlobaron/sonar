/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

import java.util.Collections;
import java.util.List;

/**
 * @since 3.0
 */
public class DatabaseVersion implements BatchComponent, ServerComponent {

  public static final int LAST_VERSION = 303;

  public static enum Status {
    UP_TO_DATE, REQUIRES_UPGRADE, REQUIRES_DOWNGRADE, FRESH_INSTALL
  }

  private MyBatis mybatis;

  public DatabaseVersion(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public Integer getVersion() {
    SqlSession session = mybatis.openSession();
    try {
      List<Integer> versions = session.getMapper(SchemaMigrationMapper.class).selectVersions();
      if (!versions.isEmpty()) {
        Collections.sort(versions);
        return versions.get(versions.size() - 1);
      }
      return null;
    } catch (RuntimeException e) {
      // The table SCHEMA_MIGRATIONS does not exist.
      // Ignore this exception -> it will be created by Ruby on Rails migrations.
      return null;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Status getStatus() {
    return getStatus(getVersion(), LAST_VERSION);
  }

  @VisibleForTesting
  static Status getStatus(Integer currentVersion, int lastVersion) {
    Status status = Status.FRESH_INSTALL;
    if (currentVersion != null) {
      if (currentVersion == lastVersion) {
        status = Status.UP_TO_DATE;
      } else if (currentVersion > lastVersion) {
        status = Status.REQUIRES_DOWNGRADE;
      } else {
        status = Status.REQUIRES_UPGRADE;
      }
    }
    return status;
  }
}
