package com.java2nb.novel.service.impl;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.java2nb.novel.core.config.OssProperties;
import com.java2nb.novel.core.utils.Constants;
import com.java2nb.novel.core.utils.FileUtil;
import com.java2nb.novel.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author 11797
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pic.save", name = "storage", havingValue = "OSS")
@Slf4j
public class OssFileServiceImpl implements FileService {

    private final OssProperties ossProperties;

    @Override
    public String transFile(String picSrc, String picSavePath) {
        log.info("OssFileServiceImpl.transFile: start remote={}", picSrc);
        File file;
        String filePath = FileUtil.network2Local(picSrc, picSavePath, Constants.LOCAL_PIC_PREFIX);
        log.info("OssFileServiceImpl.transFile: after download tempPath={}", filePath);
        if (filePath.contains(Constants.LOCAL_PIC_PREFIX)) {
            file = new File(picSavePath + filePath);
        } else {
            log.warn("OssFileServiceImpl.transFile: skip OSS upload (not local temp), return path={}", filePath);
            return filePath;
        }

        filePath = filePath.replaceFirst(picSavePath, "");

        filePath = filePath.startsWith("/") ? filePath.replaceFirst("/", "") : filePath;

        OSSClient ossClient = new OSSClient(ossProperties.getEndpoint(), ossProperties.getKeyId(), ossProperties.getKeySecret());
        try {
            if (!ossClient.doesBucketExist(ossProperties.getBucketName())) {
                ossClient.createBucket(ossProperties.getBucketName());
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(ossProperties.getBucketName());
                createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
                ossClient.createBucket(createBucketRequest);
            }
            log.info("OssFileServiceImpl.transFile: putObject bucket={} key={} file={}", ossProperties.getBucketName(),
                filePath, file.getAbsolutePath());
            PutObjectResult result = ossClient.putObject(new PutObjectRequest(ossProperties.getBucketName(), filePath, file));
            ossClient.setBucketAcl(ossProperties.getBucketName(), CannedAccessControlList.PublicRead);

            if (result != null) {
                String webUrl = ossProperties.getWebUrl() + "/" + filePath;
                log.info("OssFileServiceImpl.transFile: ok webUrl={}", webUrl);
                return webUrl;
            }
            log.warn("OssFileServiceImpl.transFile: putObject result null, fallback=/images/default.gif key={}", filePath);
        } catch (Exception e) {
            log.error("OssFileServiceImpl.transFile: OSS upload failed, fallback=/images/default.gif key={}", filePath, e);
        } finally {
            ossClient.shutdown();
            if (file.delete()) {
                log.info("OssFileServiceImpl.transFile: temp file deleted {}", file.getAbsolutePath());
            } else {
                log.warn("OssFileServiceImpl.transFile: temp file delete failed {}", file.getAbsolutePath());
            }
        }

        return "/images/default.gif";
    }


}
