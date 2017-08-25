/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.function.app;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.cloud.function.context.FunctionRegistration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registers beans that will be picked up by spring-cloud-function-context magic.
 *
 * <p>Resolves jar location provided by the user using a flexible ResourceLoader.</p>
 *
 * @author Eric Bottard
 */
@Configuration
@EnableConfigurationProperties
public class FunctionAppConfiguration {

    @Autowired
    private DelegatingResourceLoader delegatingResourceLoader;

    @Autowired
    private FunctionProperties functionProperties;

    @Bean
    @ConfigurationProperties("maven")
    public MavenProperties mavenProperties() {
        return new MavenProperties();
    }

    @Bean
    @ConfigurationProperties("function")
    public FunctionProperties functionProperties() {
        return new FunctionProperties();
    }

    @Bean
    @ConditionalOnMissingBean(DelegatingResourceLoader.class)
    public DelegatingResourceLoader delegatingResourceLoader(MavenProperties mavenProperties) {
        Map<String, ResourceLoader> loaders = new HashMap<>();
        loaders.put(MavenResource.URI_SCHEME, new MavenResourceLoader(mavenProperties));
        return new DelegatingResourceLoader(loaders);
    }

    @Bean
    public Map<String, FunctionRegistration<?>> registrations() throws Exception {
		URL[] urls = Arrays.stream(functionProperties.getLocation())
				.map(toResourceURL())
				.toArray(URL[]::new);

        try (URLClassLoader cl = new URLClassLoader(urls, this.getClass().getClassLoader())) {
            Object target = BeanUtils.instantiateClass(cl.loadClass(functionProperties.getClassName()));
            FunctionRegistration functionRegistration = new FunctionRegistration(target);
            return Collections.singletonMap("function-bean", functionRegistration);
        }
    }

    private Function<String, URL> toResourceURL() {
        return l -> {
            try {
                return delegatingResourceLoader.getResource(l).getFile().toURI().toURL();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
