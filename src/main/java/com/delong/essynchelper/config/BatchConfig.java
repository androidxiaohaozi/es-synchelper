package com.delong.essynchelper.config;

import com.delong.essynchelper.batch.*;
import com.delong.essynchelper.entity.ApplyPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class BatchConfig {

    private Logger logger = LoggerFactory.getLogger(BatchConfig.class);

    @Value("${spring.batch.job.commit-interval}")
    private int commitInterval;

    //失败时重试次数
    @Value("${spring.batch.job.retry.limit}")
    private int retryLimit;
    //#失败重试间隔（毫秒）
    @Value("${spring.batch.job.retry.back-off-period}")
    private int backOffPeriod;

    @Bean
    BatchConfigurer configurer(DataSource dataSource) {
        return new DefaultBatchConfigurer(dataSource);
    }

    @Bean("myJobRepository")
    public JobRepository myJobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setDataSource(dataSource);
        jobRepositoryFactoryBean.setTransactionManager(transactionManager);
        jobRepositoryFactoryBean.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        jobRepositoryFactoryBean.setDatabaseType("MYSQL");
        jobRepositoryFactoryBean.afterPropertiesSet();
        return jobRepositoryFactoryBean.getObject();
    }

    @Bean("simpleJobLauncher")
    public SimpleJobLauncher simpleJobLauncher(@Qualifier("myJobRepository") JobRepository myJobRepository,
                                               @Qualifier("taskExecutor") TaskExecutor taskExecutor){
        SimpleJobLauncher  simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(myJobRepository);
        simpleJobLauncher.setTaskExecutor(taskExecutor);
        try {
            simpleJobLauncher.afterPropertiesSet();
        } catch (Exception e) {
            logger.error("创建simpleJobLauncher异常：{}",e.getMessage());
        }
        return simpleJobLauncher;
    }


    @Bean
    @StepScope
    public JdbcPagingItemReader<ApplyPo> reader(@Qualifier("policyDataSource")DataSource policyDataSource,
                                                @Value("#{jobParameters}") Map<String,Object> jobParameters){
//        JdbcCursorItemReader<ApplyPo> reader = new JdbcCursorItemReader<>();
//        reader.setDataSource(policyDataSource);
//        reader.setQueryTimeout(600000);
        String sql = getPolicySql();
//        String whereSql = " " +jobParameters.get("whereSql");
//        if (!StringUtils.isEmpty(whereSql)){
//            sql+=whereSql;
//        }
//        reader.setSql(sql);
//        reader.setRowMapper(new BeanPropertyRowMapper<>(ApplyPo.class));
//        return reader;


        JdbcPagingItemReader<ApplyPo> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(policyDataSource);
        reader.setFetchSize(10);
        //把从数据库读取到的数据转成applypo对象
        reader.setRowMapper(new RowMapper<ApplyPo>() {
            @Override
            public ApplyPo mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                ApplyPo applyPo = new ApplyPo();
                applyPo.setApplyId(resultSet.getLong(1));
                applyPo.setBatchId(resultSet.getString(2));
                applyPo.setBatchName(resultSet.getString(3));
                applyPo.setSchoolName(resultSet.getString(4));
                applyPo.setApplStatus(resultSet.getInt(5));
                return applyPo;
            }
        });

        //指定sql语句
        MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
        provider.setSelectClause(sql);
        provider.setFromClause("from t_sa_apply");

        //指定根据那个字段进行排序
        Map<String, Order> sort = new HashMap<>(1);
        sort.put("f_apply_id", Order.ASCENDING);
        provider.setSortKeys(sort);

        reader.setQueryProvider(provider);
        return reader;

    }

    @Bean
    public ItemWriter<ApplyPo> writer(){
        PolicyESWriter writer = new PolicyESWriter();
        writer.setJestClient(ESClientFactory.getRestHighLevelClient());
        return writer;
    }

    @Bean
    public Job job(JobBuilderFactory jobBuilderFactory, JobRegistry jobRegistry, @Qualifier("policyStep")Step policyStep,
                   PolicyJobExecutionListener policyJobExecutionListener) throws DuplicateJobException {
        Job job =  jobBuilderFactory.get("job")
                .incrementer(new RunIdIncrementer())
                .listener(policyJobExecutionListener)
                .flow(policyStep)
                .end()
                .build();
        jobRegistry.register(new ReferenceJobFactory(job));
        return job;
    }

    @Bean
    public Step policyStep(StepBuilderFactory stepBuilderFactory, ItemReader<ApplyPo> reader,
                           ItemWriter<ApplyPo> writer, PolicySkipListener policySkipListener,
                           PolicyItemWriteListener policyItemWriteListener, PolicyESProcessor policyESProcessor) {

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backOffPeriod); //失败重试间隔1s

        return stepBuilderFactory.get("policyStep")
                .<ApplyPo, ApplyPo> chunk(commitInterval)
                .reader(reader)
                .processor(policyESProcessor)
                .writer(writer)
                .faultTolerant().retry(Exception.class).retryLimit(retryLimit)
                .backOffPolicy(backOffPolicy) //失败重试的间隔
                .skip(Exception.class).skipLimit(Integer.MAX_VALUE)
                .listener(policySkipListener)
                .listener(policyItemWriteListener)
                .build();
    }

    private String getPolicySql() {
        ClassPathResource classPathResource = new ClassPathResource("queryPolicy.sql");
        try (InputStream inputStream = classPathResource.getInputStream()){
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            return new String(bytes);
        } catch (IOException e) {
            logger.error("找不到sql配置文件[classpath:queryPolicy.sql]",e);
            return null;
        }
    }

    @BeforeStep
    public void beforeStep(final StepExecution stepExecution) {
        JobParameters parameters = stepExecution.getJobExecution().getJobParameters();
        //use your parameters
    }
}
