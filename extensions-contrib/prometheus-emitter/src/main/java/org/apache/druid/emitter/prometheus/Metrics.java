/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.emitter.prometheus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;
import org.apache.druid.java.util.common.ISE;
import org.apache.druid.java.util.common.StringUtils;
import org.apache.druid.java.util.common.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

public class Metrics
{

  private static final Logger log = new Logger(Metrics.class);
  private final Map<String, DimensionsAndCollector> map = new HashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();

  public DimensionsAndCollector getByName(String name)
  {
    return map.get(name);
  }

  public Metrics(String namespace, String path)
  {
    Map<String, Metric> metrics = readMap(path);
    for (String name : metrics.keySet()) {
      Metric metric = metrics.get(name);
      Metric.Type type = metric.type;
      String[] dimensions = metric.dimensions.toArray(new String[0]);
      String formattedName = StringUtils.replaceChar(StringUtils.toLowerCase(name), '/', "_");
      SimpleCollector collector = null;
      if (Metric.Type.count.equals(type)) {
        collector = new Counter.Builder()
            .namespace(namespace)
            .name(formattedName)
            .labelNames(dimensions)
            .register();
      } else if (Metric.Type.gauge.equals(type)) {
        collector = new Gauge.Builder()
            .namespace(namespace)
            .name(formattedName)
            .labelNames(dimensions)
            .register();
      } else if (Metric.Type.timer.equals(type)) {
        collector = new Histogram.Builder()
            .namespace(namespace)
            .name(formattedName)
            .labelNames(dimensions)
            .buckets(.1, .25, .5, .75, 1, 2.5, 5, 7.5, 10, 30, 60, 120, 300)
            .register();
      } else {
        log.error("Unrecognized metric type [%s]", type);
      }

      if (collector != null) {
        map.put(name, new DimensionsAndCollector(dimensions, collector));
      }
    }

  }

  private Map<String, Metric> readMap(String path)
  {
    try {
      InputStream is;
      if (Strings.isNullOrEmpty(path)) {
        log.info("Using default metric dimension and types");
        is = this.getClass().getClassLoader().getResourceAsStream("defaultMetrics.json");
      } else {
        log.info("Using metric dimensions at types at [%s]", path);
        is = new FileInputStream(new File(path));
      }
      return mapper.readerFor(new TypeReference<Map<String, Metric>>()
      {
      }).readValue(is);
    }
    catch (IOException e) {
      throw new ISE(e, "Failed to parse metric dimensions and types");
    }
  }

  public static class Metric
  {
    public final SortedSet<String> dimensions;
    public final Type type;

    @JsonCreator
    public Metric(
        @JsonProperty("dimensions") SortedSet<String> dimensions,
        @JsonProperty("type") Type type
    )
    {
      this.dimensions = dimensions;
      this.type = type;
    }

    public enum Type
    {
      count, gauge, timer
    }
  }
}
