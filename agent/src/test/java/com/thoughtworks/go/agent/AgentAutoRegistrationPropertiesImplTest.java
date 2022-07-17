/*
 * Copyright 2022 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.agent;

import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class AgentAutoRegistrationPropertiesImplTest {

    private File configFile;

    @BeforeEach
    void setUp(@TempDir Path folder) throws IOException {
        configFile = Files.createFile(folder.resolve("config.properties")).toFile();
    }

    @Test
    void shouldReturnAgentAutoRegisterPropertiesIfPresent() throws Exception {
        Properties properties = new Properties();

        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "foo");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "foo, zoo");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "foo, bar");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
        try (OutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "");
        }

        AgentAutoRegistrationProperties reader = new AgentAutoRegistrationPropertiesImpl(configFile);
        assertThat(reader.agentAutoRegisterKey()).isEqualTo("foo");
        assertThat(reader.agentAutoRegisterResources()).isEqualTo("foo, zoo");
        assertThat(reader.agentAutoRegisterEnvironments()).isEqualTo("foo, bar");
        assertThat(reader.agentAutoRegisterHostname()).isEqualTo("agent01.example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\r", "\n", "\r\n"})
    void shouldAllowDifferentLineEndings(String lineEnding) throws IOException {
        List<String> lines = List.of(
            AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY + "=foo",
            AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES + "=bar"
        );

        FileUtils.writeStringToFile(configFile, String.join(lineEnding, lines), UTF_8);

        AgentAutoRegistrationProperties reader = new AgentAutoRegistrationPropertiesImpl(configFile);
        assertThat(reader.agentAutoRegisterKey()).isEqualTo("foo");
        assertThat(reader.agentAutoRegisterResources()).isEqualTo("bar");
    }

    @Test
    void shouldReturnEmptyStringIfPropertiesNotPresent() {
        AgentAutoRegistrationProperties reader = new AgentAutoRegistrationPropertiesImpl(configFile);
        assertThat(reader.agentAutoRegisterKey().isEmpty()).isTrue();
        assertThat(reader.agentAutoRegisterResources().isEmpty()).isTrue();
        assertThat(reader.agentAutoRegisterEnvironments().isEmpty()).isTrue();
        assertThat(reader.agentAutoRegisterHostname().isEmpty()).isTrue();
    }

    @Test
    void shouldScrubTheAutoRegistrationProperties() throws Exception {
        String originalContents = "" +
            "#\n" +
            "# file autogenerated by chef, any changes will be lost\n" +
            "#\n" +
            "# the registration key\n" +
            "agent.auto.register.key = some secret key\n" +
            "\n" +
            "# the resources on this agent\n" +
            "agent.auto.register.resources = some,resources\n" +
            "\n" +
            "# The hostname of this agent\n" +
            "agent.auto.register.hostname = agent42.example.com\n" +
            "\n" +
            "# The environments this agent belongs to\n" +
            "agent.auto.register.environments = production,blue\n" +
            "\n";
        FileUtils.writeStringToFile(configFile, originalContents, UTF_8);

        AgentAutoRegistrationProperties properties = new AgentAutoRegistrationPropertiesImpl(configFile);
        properties.scrubRegistrationProperties();

        String newContents = "" +
            "#\n" +
            "# file autogenerated by chef, any changes will be lost\n" +
            "#\n" +
            "# the registration key\n" +
            "# The autoregister key has been intentionally removed by Go as a security measure.\n" +
            "# agent.auto.register.key = some secret key\n" +
            "\n" +
            "# the resources on this agent\n" +
            "# This property has been removed by Go after attempting to auto-register with the Go server.\n" +
            "# agent.auto.register.resources = some,resources\n" +
            "\n" +
            "# The hostname of this agent\n" +
            "# This property has been removed by Go after attempting to auto-register with the Go server.\n" +
            "# agent.auto.register.hostname = agent42.example.com\n" +
            "\n" +
            "# The environments this agent belongs to\n" +
            "# This property has been removed by Go after attempting to auto-register with the Go server.\n" +
            "# agent.auto.register.environments = production,blue\n" +
            "\n";
        assertThat(FileUtils.readFileToString(configFile, UTF_8)).isEqualTo(newContents);
    }
}
