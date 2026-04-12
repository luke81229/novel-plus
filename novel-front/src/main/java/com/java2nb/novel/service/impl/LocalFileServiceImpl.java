package com.java2nb.novel.service.impl;

import com.java2nb.novel.core.utils.Constants;
import com.java2nb.novel.core.utils.FileUtil;
import com.java2nb.novel.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author 11797
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pic.save", name = "storage", havingValue = "local")
@Slf4j
public class LocalFileServiceImpl implements FileService {

    @Override
    public String transFile(String picSrc, String picSavePath) {
        log.info("LocalFileServiceImpl.transFile: storage=local remote={}", picSrc);
        String out = FileUtil.network2Local(picSrc, picSavePath, Constants.LOCAL_PIC_PREFIX);
        log.info("LocalFileServiceImpl.transFile: result path={}", out);
        return out;
    }
}
