package com.delong.essynchelper;

import com.delong.essynchelper.batch.PolicyESWriter;
import com.delong.essynchelper.entity.CommonPo;
import com.delong.essynchelper.util.JsonUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class EsSynchelperApplicationTests {

    private Logger logger = LoggerFactory.getLogger(PolicyESWriter.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Test
    void contextLoads() {
        List<CommonPo> list = new ArrayList<>();
        CommonPo commonPo = new CommonPo();
        commonPo.setELECURTIMESTAMP("2023-04-07 07:39:48");
        commonPo.setELECURVALUE("14.5");
        commonPo.setId(1);
        list.add(commonPo);

        commonPo = new CommonPo();
        commonPo.setELECURTIMESTAMP("2023-04-07 05:11:39");
        commonPo.setELECURVALUE("61.45");
        commonPo.setId(2);
        list.add(commonPo);

        logger.info("PolicyESWriter list.size() = "+list.size());
        BulkRequest bulkRequest = new BulkRequest();
        Document annotation = CommonPo.class.getAnnotation(Document.class);
        //TODO 日期格式处理 https://jtruty.github.io/programming/2015/04/03/elasticsearch-http-queries-with-jest.html
        //需要的日期 https://stackoverflow.com/questions/41365704/elasticsearch-jest-date-serialization-java
        //参考 https://segmentfault.com/a/1190000016726694
        for (CommonPo vo : list){
            IndexRequest indexRequest = new IndexRequest(annotation.indexName());
            String source = JsonUtil.toJson(vo);
            indexRequest.id(vo.getId().toString());
            indexRequest.source(source, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        try {
            restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("PolicyESWriter jestClient execute bulk size = "+list.size());
    }

}
