/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.agent.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.thoughtworks.go.config.AgentRegistrationPropertiesReader;
import com.thoughtworks.go.config.DefaultAgentRegistry;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.Mockito;
import org.springframework.util.StreamUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

public class RemoteRegistrationRequesterTest {
    private Properties original;

    @Before
    public void before() {
        original = new Properties(System.getProperties());
    }

    @After
    public void after() {
        System.setProperties(original);
    }

    @Test
    public void shouldPassAllParametersToPostForRegistration() throws IOException, ClassNotFoundException {
        new SystemEnvironment().setProperty("os.name", "minix");
        String url = "http://cruise.com/go";
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry).requestRegistration("cruise.com", new AgentRegistrationPropertiesReader(properties));
        verify(httpClient).execute(argThat(hasAllParams(defaultAgentRegistry.uuid())));
    }

    private TypeSafeMatcher<HttpPost> hasAllParams(final String uuid) {
        return new TypeSafeMatcher<HttpPost>() {
            @Override public boolean matchesSafely(HttpPost item) {
                HttpPost postMethod = (HttpPost) item;
                List<NameValuePair> pairs = null;
                try {
                    pairs = URLEncodedUtils.parse(postMethod.getEntity());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Map<String, String> params = new HashMap<String, String>();
                for (NameValuePair p : pairs) {
                    params.put(p.getName(), p.getValue());
                }

                assertThat(params.get("hostname"),is("cruise.com"));

                assertThat(params.get("uuid"),is(uuid));
                String workingDir = SystemUtil.currentWorkingDirectory();
                assertThat(params.get("location"),is(workingDir));
                assertThat(params.get("operating_system"),is("minix"));
                assertThat(params.get("agentAutoRegisterKey"),is("t0ps3cret"));
                assertThat(params.get("agentAutoRegisterResources"),is("linux, java"));
                assertThat(params.get("agentAutoRegisterEnvironments"),is("uat, staging"));
                assertThat(params.get("agentAutoRegisterHostname"),is("agent01.example.com"));
                return true;
            }

            public void describeTo(Description description) {
                description.appendText("params containing");
            }
        };
    }

    private SslInfrastructureService.RemoteRegistrationRequester remoteRegistryRequester(final String url, final HttpClient httpClient, final DefaultAgentRegistry defaultAgentRegistry) {
        return new SslInfrastructureService.RemoteRegistrationRequester(url, defaultAgentRegistry, httpClient){
            @Override protected Registration readResponse(InputStream is) throws IOException, ClassNotFoundException {
                return null;
            }
        };
    }


}
