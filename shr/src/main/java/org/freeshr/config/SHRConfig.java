package org.freeshr.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
@Import({SHRSecurityConfig.class, SHRCassandraConfig.class,
        SHRCacheConfiguration.class, ActuatorConfig.class})
@ComponentScan(basePackages = {"org.freeshr.config",
        "org.freeshr.infrastructure",
        "org.freeshr.interfaces",
        "org.freeshr.domain",
        "org.freeshr.application.fhir",
        "org.freeshr.validations"})
public class SHRConfig {
    @Autowired
    private SHRProperties shrProperties;

    @Bean(name = "SHRRestTemplate")
    public AsyncRestTemplate shrRestTemplate() {
        /*TODO: See whether ThreadPoolExecutor is the best for the job*/
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        executor.setCorePoolSize(shrProperties.getRestPoolSize());
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(executor);
        AsyncClientHttpRequestFactory asyncRequestFactory = asyncRestTemplate.getAsyncRequestFactory();
        if (asyncRequestFactory instanceof SimpleClientHttpRequestFactory) {
            setRequestTimeOuts((SimpleClientHttpRequestFactory) asyncRequestFactory);
        }
        return asyncRestTemplate;
    }

    private void setRequestTimeOuts(SimpleClientHttpRequestFactory asyncRequestFactory) {
        asyncRequestFactory.setConnectTimeout(shrProperties.getServerConnectionTimeout());
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /*TODO: Verify whether the codes are needed for validations by writing tests and then remove this bean if they
    are not.*/
    @Bean(name = "hl7CodeProperties")
    public static PropertiesFactoryBean hl7() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("/hl7codes.properties"));
        return bean;
    }

    @Bean(name = "fhirTrMap")
    public Properties fhirTrMap() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/fhir-tr-mapping.properties");
        Properties properties = new Properties();
        properties.load(inputStream);
        populateTRUrls(properties);
        return properties;

    }

    private void populateTRUrls(Properties properties) {
        for (Object key : properties.keySet()) {
            properties.put(key, shrProperties.getTrServerBaseUrl() + properties.get(key));
        }
    }
}
