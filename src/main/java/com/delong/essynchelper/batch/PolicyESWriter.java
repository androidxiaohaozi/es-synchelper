package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.CommonPo;
import com.delong.essynchelper.entity.TspReceiveDataPo;
import com.delong.essynchelper.util.JsonUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

public class PolicyESWriter implements ItemWriter<TspReceiveDataPo> {

    private Logger logger = LoggerFactory.getLogger(PolicyESWriter.class);

    private RestHighLevelClient restHighLevelClient;

    public void setJestClient(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    @Override
    public void write(List<? extends TspReceiveDataPo> list) throws Exception {
        logger.info("PolicyESWriter list.size() = "+list.size());
        BulkRequest bulkRequest = new BulkRequest();
        Document annotation = TspReceiveDataPo.class.getAnnotation(Document.class);
        //TODO 日期格式处理 https://jtruty.github.io/programming/2015/04/03/elasticsearch-http-queries-with-jest.html
        //需要的日期 https://stackoverflow.com/questions/41365704/elasticsearch-jest-date-serialization-java
       //参考 https://segmentfault.com/a/1190000016726694
        for (TspReceiveDataPo vo : list){
            IndexRequest indexRequest = new IndexRequest(annotation.indexName());
            String source = JsonUtil.toJson(vo);
            indexRequest.id(vo.getId().toString());
            indexRequest.source(source, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

        logger.info("PolicyESWriter jestClient execute bulk size = "+list.size());
    }


}
