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

import com.thoughtworks.go.agent.ResetableHttpClient;
import com.thoughtworks.go.config.AgentRegistrationPropertiesReader;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.config.GuidService;
import com.thoughtworks.go.security.AuthSSLProtocolSocketFactory;
import com.thoughtworks.go.security.KeyStoreManager;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.URLService;

import org.apache.commons.io.FileUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.security.CertificateUtil.md5Fingerprint;
import static com.thoughtworks.go.security.SelfSignedCertificateX509TrustManager.CRUISE_SERVER;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Service
public class SslInfrastructureService {

    public static final File AGENT_CERTIFICATE_FILE = new File("config", "agent.jks");
    public static final File AGENT_TRUST_FILE = new File("config", "trust.jks");
    public static final String CHAIN_ALIAS = "agent";
    static final String AGENT_STORE_PASSWORD = "agent5s0repa55w0rd";
    private static final Logger LOGGER = Logger.getLogger(SslInfrastructureService.class);
    private static final int REGISTER_RETRY_INTERVAL = 5000;
    private final RemoteRegistrationRequester remoteRegistrationRequester;
    private final KeyStoreManager keyStoreManager;
    private final ResetableHttpClient httpClient;
    private AuthSSLProtocolSocketFactory protocolSocketFactory;
    private transient boolean registered = false;

    @Autowired
    public SslInfrastructureService(URLService urlService, ResetableHttpClient httpClient, AgentRegistry agentRegistry) throws Exception {
        this(new RemoteRegistrationRequester(urlService.getAgentRegistrationURL(), agentRegistry, httpClient), httpClient);
    }

    // For mocking out remote call
    SslInfrastructureService(RemoteRegistrationRequester requester, ResetableHttpClient httpClient)
            throws Exception {
        this.remoteRegistrationRequester = requester;
        this.httpClient = httpClient;
        this.keyStoreManager = new KeyStoreManager();
        this.keyStoreManager.preload(AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD);
    }

    public void createSslInfrastructure() throws IOException {
        File parentFile = AGENT_TRUST_FILE.getParentFile();
        if (parentFile.exists() || parentFile.mkdirs()) {
            protocolSocketFactory = new AuthSSLProtocolSocketFactory(
                    AGENT_TRUST_FILE, AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD);
            protocolSocketFactory.registerAsHttpsProtocol();
            this.httpClient.setSslContext(protocolSocketFactory.getSSLContext());
        } else {
            bomb("Unable to create folder " + parentFile.getAbsolutePath());
        }
    }

    public void registerIfNecessary() throws Exception {
        registered = keyStoreManager.hasCertificates(CHAIN_ALIAS, AGENT_CERTIFICATE_FILE,
                AGENT_STORE_PASSWORD) && GuidService.guidPresent();
        if (!registered) {
            LOGGER.info("[Agent Registration] Starting to register agent");
            register();
            createSslInfrastructure();
            registered = true;
            LOGGER.info("[Agent Registration] Successfully registered agent");
        }
    }

    public boolean isRegistered() {
        return registered;
    }

    private void register() throws Exception {
        String hostName = SystemUtil.getLocalhostNameOrRandomNameIfNotFound();
        Registration keyEntry = null;
        while (keyEntry == null || keyEntry.getChain().length == 0) {
            File autoRegisterPropertiesFile = new File("config", "autoregister.properties");
            AgentRegistrationPropertiesReader agentRegistrationPropertiesReader = new AgentRegistrationPropertiesReader(autoRegisterPropertiesFile);
            try {
                keyEntry = remoteRegistrationRequester.requestRegistration(hostName, agentRegistrationPropertiesReader);
            } catch (Exception e) {
                LOGGER.error("[Agent Registration] Problems getting the Agent Certificate from Go Server", e);
                throw e;
            } finally {
                agentRegistrationPropertiesReader = null;
                if (autoRegisterPropertiesFile != null && autoRegisterPropertiesFile.exists()) {
                    if (FileUtils.deleteQuietly(autoRegisterPropertiesFile)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("[Agent Auto Registration] Successfully deleted auto registration properties file on agent.");
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("[Agent Auto Registration] Failed deleting auto registration properties file on agent.");
                        }
                    }
                }
            }

            try {
                Thread.sleep(REGISTER_RETRY_INTERVAL);
            } catch (InterruptedException e) {
                // Ok
            }
        }
        LOGGER.info("[Agent Registration] Retrieved registration from Go server.");
        storeChainIntoAgentStore(keyEntry);
    }

    void storeChainIntoAgentStore(Registration keyEntry) {
        try {
            keyStoreManager.storeCertificate(CHAIN_ALIAS, AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD, keyEntry);
            LOGGER.info(String.format("[Agent Registration] Stored registration for cert with hash code: %s not valid before: %s", md5Fingerprint(keyEntry.getFirstCertificate()),
                    keyEntry.getCertificateNotBeforeDate()));
        } catch (Exception e) {
            throw bomb("Couldn't save agent key into store", e);
        }
    }

    public void invalidateAgentCertificate() {
        httpClient.reset();
        try {
            keyStoreManager.deleteEntry(CHAIN_ALIAS, AGENT_CERTIFICATE_FILE, AGENT_STORE_PASSWORD);
            keyStoreManager.deleteEntry(CRUISE_SERVER, AGENT_TRUST_FILE, AGENT_STORE_PASSWORD);
        } catch (Exception e) {
            LOGGER.fatal("[Agent Registration] Error while deleting key from key store", e);
            deleteKeyStores();
        }
    }

    public void deleteKeyStores() {
        FileUtils.deleteQuietly(AGENT_CERTIFICATE_FILE);
        FileUtils.deleteQuietly(AGENT_TRUST_FILE);
    }

    public static class RemoteRegistrationRequester {
        private final AgentRegistry agentRegistry;
        private String serverUrl;
        private HttpClient httpClient;

        public RemoteRegistrationRequester(String serverUrl, AgentRegistry agentRegistry, HttpClient httpClient) {
            this.serverUrl = serverUrl;
            this.httpClient = httpClient;
            this.agentRegistry = agentRegistry;
        }

        protected Registration requestRegistration(String agentHostName, AgentRegistrationPropertiesReader agentAutoRegisterProperties) throws IOException, ClassNotFoundException {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("[Agent Registration] Using URL %s to register.", serverUrl));
            }
            HttpPost postMethod = new HttpPost(serverUrl);

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();

            nvps.add(new BasicNameValuePair("hostname", agentHostName));
            nvps.add(new BasicNameValuePair("uuid", agentRegistry.uuid()));
            String workingdir = SystemUtil.currentWorkingDirectory();
            nvps.add(new BasicNameValuePair("location", workingdir));
            nvps.add(new BasicNameValuePair("usablespace",
                    String.valueOf(AgentRuntimeInfo.usableSpace(workingdir))));
            nvps.add(new BasicNameValuePair("operating_system", new SystemEnvironment().getOperatingSystemName()));
            nvps.add(new BasicNameValuePair("agentAutoRegisterKey", agentAutoRegisterProperties.getAgentAutoRegisterKey()));
            nvps.add(new BasicNameValuePair("agentAutoRegisterResources", agentAutoRegisterProperties.getAgentAutoRegisterResources()));
            nvps.add(new BasicNameValuePair("agentAutoRegisterEnvironments", agentAutoRegisterProperties.getAgentAutoRegisterEnvironments()));
            nvps.add(new BasicNameValuePair("agentAutoRegisterHostname", agentAutoRegisterProperties.getAgentAutoRegisterHostname()));

            postMethod.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
            try {
                HttpResponse response = httpClient.execute(postMethod);
                InputStream is = response.getEntity().getContent();
                return readResponse(is);
            } finally {
                postMethod.releaseConnection();
            }
        }

        protected Registration readResponse(InputStream is) throws IOException, ClassNotFoundException {
            return (Registration) new ObjectInputStream(is).readObject();
        }
    }
}
