package com.monteweb.search.internal.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SolrConfig {

    @Value("${monteweb.solr.url}")
    private String solrUrl;

    @Bean
    public SolrClient solrClient() {
        return new HttpJdkSolrClient.Builder(solrUrl).build();
    }
}
