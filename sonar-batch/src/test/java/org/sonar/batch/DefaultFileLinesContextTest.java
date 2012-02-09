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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DefaultFileLinesContextTest {

  private SonarIndex index;
  private Resource resource;
  private DefaultFileLinesContext fileLineMeasures;

  @Before
  public void setUp() {
    index = mock(SonarIndex.class);
    resource = mock(Resource.class);
    fileLineMeasures = new DefaultFileLinesContext(index, resource);
  }

  @Test
  public void shouldSave() {
    fileLineMeasures.setIntValue("hits", 1, 2);
    fileLineMeasures.setIntValue("hits", 3, 4);
    fileLineMeasures.save();

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    verify(index).addMeasure(Mockito.eq(resource), measureCaptor.capture());
    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetricKey(), is("hits"));
    assertThat(measure.getPersistenceMode(), is(PersistenceMode.DATABASE));
    assertThat(measure.getData(), is("1=2;3=4"));
  }

  @Test
  public void shouldSaveSeveral() {
    fileLineMeasures.setIntValue("hits", 1, 2);
    fileLineMeasures.setIntValue("hits", 3, 4);
    fileLineMeasures.setStringValue("author", 1, "simon");
    fileLineMeasures.setStringValue("author", 3, "evgeny");
    fileLineMeasures.save();

    verify(index, times(2)).addMeasure(Mockito.eq(resource), Mockito.any(Measure.class));
  }

  @Test
  public void shouldLoadIntValues() {
    when(index.getMeasure(Mockito.any(Resource.class), Mockito.any(Metric.class)))
        .thenReturn(new Measure("hits").setData("1=2;3=4"));

    assertThat(fileLineMeasures.getIntValue("hits", 1), is(2));
    assertThat(fileLineMeasures.getIntValue("hits", 3), is(4));
    assertThat("no measure on line", fileLineMeasures.getIntValue("hits", 5), nullValue());
  }

  @Test
  public void shouldLoadStringValues() {
    when(index.getMeasure(Mockito.any(Resource.class), Mockito.any(Metric.class)))
        .thenReturn(new Measure("author").setData("1=simon;3=evgeny"));

    assertThat(fileLineMeasures.getStringValue("author", 1), is("simon"));
    assertThat(fileLineMeasures.getStringValue("author", 3), is("evgeny"));
    assertThat("no measure on line", fileLineMeasures.getStringValue("author", 5), nullValue());
  }

  @Test
  public void shouldNotSaveAfterLoad() {
    when(index.getMeasure(Mockito.any(Resource.class), Mockito.any(Metric.class)))
        .thenReturn(new Measure("author").setData("1=simon;3=evgeny"));

    fileLineMeasures.getStringValue("author", 1);
    fileLineMeasures.save();

    verify(index, never()).addMeasure(Mockito.eq(resource), Mockito.any(Measure.class));
  }

  @Test
  public void shouldNotFailIfNoMeasureInIndex() {
    assertThat(fileLineMeasures.getIntValue("hits", 1), nullValue());
    assertThat(fileLineMeasures.getStringValue("author", 1), nullValue());
  }

}