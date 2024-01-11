package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.TspReceiveDataPo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PolicyLogFileHelper {

    private Logger logger = LoggerFactory.getLogger(PolicyLogFileHelper.class);

    @Value("policy.log.filepath")
    private String logFilePath;

    //每个文件的最大行数
    @Value("${policy.log.max-line}")
    private Integer fileMaxLines;

    //记录jod当前文件行数
    private ConcurrentHashMap<Long, Integer> jobFileLines = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, Integer> jobFileNames = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long, FileChannel> jobFileChannels = new ConcurrentHashMap<>();

    void write(Long jobId, List<? extends TspReceiveDataPo> list) {
        FileChannel fileChannel = getCurrentFileChannel(jobId);
        Integer lines = jobFileLines.get(jobId);
        for (TspReceiveDataPo vo : list) {
            ByteBuffer src = ByteBuffer.wrap((vo.getId() + "\n").getBytes());
            try {
                fileChannel.write(src);
            } catch (IOException e) {
                logger.error("", e);
            }
            lines++;
            if (lines >= fileMaxLines) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
                Integer fileno = jobFileNames.get(jobId);
                fileno++;
                String filename = StringUtils.leftPad(fileno + ".txt", 8, "0");
                try {
                    fileChannel = new RandomAccessFile(logFilePath + File.separator
                            + jobId + File.separator + filename, "rw").getChannel();
                } catch (FileNotFoundException e) {
                    logger.error("", e);
                }
                lines = 0;
                jobFileNames.put(jobId, fileno);
                jobFileLines.put(jobId, lines);
                jobFileChannels.put(jobId, fileChannel);
            }
        }
    }


    private FileChannel getCurrentFileChannel(Long jobId) {
        FileChannel fileChannel = jobFileChannels.get(jobId);
        if (fileChannel == null) {
            File file = new File(logFilePath + File.separator + jobId);
            if (!file.exists()) {
                file.mkdirs();
            }
            jobFileNames.put(jobId, 1);
            jobFileLines.put(jobId, 0);
            String filename = StringUtils.leftPad("1.txt", 8, "0");
            try {
                fileChannel = new RandomAccessFile(logFilePath + File.separator
                        + jobId + File.separator + filename, "rw").getChannel();
            } catch (FileNotFoundException e) {
                logger.error("", e);
            }
        }
        return fileChannel;
    }

    void clear(Long jobId) {
        FileChannel fileChannel = jobFileChannels.get(jobId);
        boolean isEmpty = false;
        if (fileChannel != null) {
            try {
                isEmpty = fileChannel.size() == 0;
                fileChannel.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        if (isEmpty) {
            Integer fileno = jobFileNames.get(jobId);
            String filename = StringUtils.leftPad(fileno + ".txt", 8, "0");
            File file = new File(logFilePath + File.separator + jobId + File.separator + filename);
            if (file.exists()) {
                file.delete();
            }
        }
        jobFileChannels.remove(jobId);
        jobFileLines.remove(jobId);
        jobFileNames.remove(jobId);
    }
}
