package com.delong.essynchelper.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

/**
 * @Description ESClientFactory
 * @Auther wanghao
 * @Date 2021/6/18 15:26
 */
public class ESClientFactory {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9200;
    private static final String SCHEMA = "http";
    private static final int CONNECT_TIME_OUT = 1000;
    private static final int SOCKET_TIME_OUT = 30000;
    private static final int CONNECTION_REQUEST_TIME_OUT = 500;

    private static final int MAX_CONNECT_NUM = 100;
    private static final int MAX_CONNECT_PER_ROUTE = 100;

    private static HttpHost HTTP_HOST = new HttpHost(HOST,PORT,SCHEMA);
    private static boolean uniqueConnectTimeConfig = false;
    private static boolean uniqueConnectNumConfig = true;
    private static RestClientBuilder builder;
    private static RestClient restClient;
    private static RestHighLevelClient restHighLevelClient;

    static{
        init();
    }


    private static void init() {
        builder = RestClient.builder(HTTP_HOST);
        if (uniqueConnectTimeConfig) {
            setConnectTimeOutConfig();
        }

        if (uniqueConnectNumConfig) {
            setMutiConnectConfig();
        }
        restClient = builder.build();
        restHighLevelClient = new RestHighLevelClient(builder);
    }


    //主要关于异步httpclient的连接延时配置
    private static void setConnectTimeOutConfig() {
        builder.setRequestConfigCallback(requestConfigBuilder -> {

            requestConfigBuilder.setConnectTimeout(CONNECT_TIME_OUT);
            requestConfigBuilder.setSocketTimeout(SOCKET_TIME_OUT);
            requestConfigBuilder.setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT);

            return requestConfigBuilder;
        });
    }

    //主要关于异步httpclient的连接数配置
    private static void setMutiConnectConfig() {
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(MAX_CONNECT_NUM);
            httpClientBuilder.setMaxConnPerRoute(MAX_CONNECT_PER_ROUTE);
            return httpClientBuilder;
        });
    }

    public static RestClient getClient() {
        return restClient;
    }

    static RestHighLevelClient getRestHighLevelClient() {
        return restHighLevelClient;
    }

    public static void close() {
        if (restHighLevelClient != null) {
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
