package com.delong.essynchelper.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "policyDataSource")
    @ConfigurationProperties(prefix="spring.datasource.main")
    public DataSource policyDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "policyJdbcTemplate")
    public JdbcTemplate policyJdbcTemplate(@Qualifier("policyDataSource") DataSource policyDataSource){
        return new JdbcTemplate(policyDataSource);
    }
}
