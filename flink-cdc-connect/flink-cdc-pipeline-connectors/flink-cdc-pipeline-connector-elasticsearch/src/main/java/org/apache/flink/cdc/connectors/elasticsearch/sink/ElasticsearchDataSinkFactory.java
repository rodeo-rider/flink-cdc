/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.cdc.connectors.elasticsearch.sink;

import org.apache.flink.cdc.common.configuration.ConfigOption;
import org.apache.flink.cdc.common.configuration.Configuration;
import org.apache.flink.cdc.common.factories.DataSinkFactory;
import org.apache.flink.cdc.common.factories.FactoryHelper;
import org.apache.flink.cdc.common.pipeline.PipelineOptions;
import org.apache.flink.cdc.common.sink.DataSink;
import org.apache.flink.cdc.connectors.elasticsearch.config.ElasticsearchSinkOptions;
import org.apache.flink.cdc.connectors.elasticsearch.v2.NetworkConfig;

import org.apache.http.HttpHost;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.flink.cdc.connectors.elasticsearch.sink.ElasticsearchDataSinkOptions.*;

/** Factory for creating {@link ElasticsearchDataSink}. */
public class ElasticsearchDataSinkFactory implements DataSinkFactory {

    public static final String IDENTIFIER = "elasticsearch";

    @Override
    public DataSink createDataSink(Context context) {
        FactoryHelper.createFactoryHelper(this, context).validate();

        Configuration configuration =
                Configuration.fromMap(context.getFactoryConfiguration().toMap());
        ZoneId zoneId = determineZoneId(context);

        ElasticsearchSinkOptions sinkOptions = buildSinkConnectorOptions(configuration);
        return new ElasticsearchDataSink(sinkOptions, zoneId);
    }

    private ZoneId determineZoneId(Context context) {
        String configuredZone =
                context.getPipelineConfiguration().get(PipelineOptions.PIPELINE_LOCAL_TIME_ZONE);
        String defaultZone = PipelineOptions.PIPELINE_LOCAL_TIME_ZONE.defaultValue();

        return Objects.equals(configuredZone, defaultZone)
                ? ZoneId.systemDefault()
                : ZoneId.of(configuredZone);
    }

    private ElasticsearchSinkOptions buildSinkConnectorOptions(Configuration cdcConfig) {
        List<HttpHost> hosts = parseHosts(cdcConfig.get(HOSTS));
        String username = cdcConfig.get(USERNAME);
        String password = cdcConfig.get(PASSWORD);
        NetworkConfig networkConfig =
                new NetworkConfig(hosts, username, password, null, null, null);
        return new ElasticsearchSinkOptions(
                cdcConfig.get(MAX_BATCH_SIZE),
                cdcConfig.get(MAX_IN_FLIGHT_REQUESTS),
                cdcConfig.get(MAX_BUFFERED_REQUESTS),
                cdcConfig.get(MAX_BATCH_SIZE_IN_BYTES),
                cdcConfig.get(MAX_TIME_IN_BUFFER_MS),
                cdcConfig.get(MAX_RECORD_SIZE_IN_BYTES),
                networkConfig);
    }

    private List<HttpHost> parseHosts(String hostsStr) {
        return Arrays.stream(hostsStr.split(","))
                .map(HttpHost::create)
                .collect(Collectors.toList());
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(HOSTS);
        requiredOptions.add(INDEX);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(MAX_BATCH_SIZE);
        optionalOptions.add(MAX_IN_FLIGHT_REQUESTS);
        optionalOptions.add(MAX_BUFFERED_REQUESTS);
        optionalOptions.add(MAX_BATCH_SIZE_IN_BYTES);
        optionalOptions.add(MAX_TIME_IN_BUFFER_MS);
        optionalOptions.add(MAX_RECORD_SIZE_IN_BYTES);
        optionalOptions.add(USERNAME);
        optionalOptions.add(PASSWORD);
        return optionalOptions;
    }
}