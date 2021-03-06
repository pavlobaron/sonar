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
package org.sonar.batch;

import java.util.Date;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

public class ProjectConfigurator implements BatchComponent {

  private DatabaseSession databaseSession;
  private Settings settings;

  public ProjectConfigurator(DatabaseSession databaseSession, Settings settings) {
    this.databaseSession = databaseSession;
    this.settings = settings;
  }

  public Project create(ProjectDefinition definition) {
    Project project = new Project(definition.getKey(), loadProjectBranch(), definition.getName());

    // For backward compatibility we must set POM and actual packaging
    project.setDescription(StringUtils.defaultString(definition.getDescription()));
    project.setPackaging("jar");

    for (Object component : definition.getContainerExtensions()) {
      if (component instanceof MavenProject) {
        MavenProject pom = (MavenProject) component;
        project.setPom(pom);
        project.setPackaging(pom.getPackaging());
      }
    }
    return project;
  }

  String loadProjectBranch() {
    return settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
  }

  public ProjectConfigurator configure(Project project) {
    Date analysisDate = loadAnalysisDate();
    project
        .setConfiguration(new PropertiesConfiguration()) // will be populated by ProjectSettings
        .setAnalysisDate(analysisDate)
        .setLatestAnalysis(isLatestAnalysis(project.getKey(), analysisDate))
        .setAnalysisVersion(loadAnalysisVersion())
        .setAnalysisType(loadAnalysisType())
        .setLanguageKey(loadLanguageKey());
    return this;
  }

  boolean isLatestAnalysis(String projectKey, Date analysisDate) {
    ResourceModel persistedProject = databaseSession.getSingleResult(ResourceModel.class, "key", projectKey, "enabled", true);
    if (persistedProject != null) {
      Snapshot lastSnapshot = databaseSession.getSingleResult(Snapshot.class, "resourceId", persistedProject.getId(), "last", true);
      return lastSnapshot == null || lastSnapshot.getCreatedAt().before(analysisDate);
    }
    return true;
  }

  Date loadAnalysisDate() {
    Date date = null;
    try {
      // sonar.projectDate may have been specified as a time
      date = settings.getDateTime(CoreProperties.PROJECT_DATE_PROPERTY);
    } catch (SonarException e) {
      // this is probably just a date
      date = settings.getDate(CoreProperties.PROJECT_DATE_PROPERTY);
    }
    if (date == null) {
      date = new Date();
    }
    return date;
  }

  Project.AnalysisType loadAnalysisType() {
    String value = settings.getString(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY);
    if (value == null) {
      return ("true".equals(settings.getString("sonar.light")) ? Project.AnalysisType.STATIC : Project.AnalysisType.DYNAMIC);
    }
    if ("true".equals(value)) {
      return Project.AnalysisType.DYNAMIC;
    }
    if ("reuseReports".equals(value)) {
      return Project.AnalysisType.REUSE_REPORTS;
    }
    return Project.AnalysisType.STATIC;
  }

  String loadAnalysisVersion() {
    return settings.getString(CoreProperties.PROJECT_VERSION_PROPERTY);
  }

  String loadLanguageKey() {
    return StringUtils.defaultIfBlank(settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY), Java.KEY);
  }
}
