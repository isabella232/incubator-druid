#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Skip stopping docker if flag set (For use during development)
if [ -n "$DRUID_INTEGRATION_TEST_SKIP_STOP_DOCKER" ] && [ "$DRUID_INTEGRATION_TEST_SKIP_STOP_DOCKER" == true ]
  then
    exit 0
  fi

for node in druid-historical druid-coordinator druid-overlord druid-router druid-router-permissive-tls druid-router-no-client-auth-tls druid-router-custom-check-tls druid-broker druid-middlemanager druid-zookeeper-kafka druid-metadata-storage druid-it-hadoop;

do
docker stop $node
docker rm $node
done

docker network rm druid-it-net
