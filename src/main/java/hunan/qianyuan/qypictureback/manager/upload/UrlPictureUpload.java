package hunan.qianyuan.qypictureback.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import hunan.qianyuan.qypictureback.exception.BusinessException;
import hunan.qianyuan.qypictureback.exception.ErrorCode;
import hunan.qianyuan.qypictureback.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {  
    @Override  
    protected void validPicture(Object inputSource) {  
        String fileUrl = (String) inputSource;  
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        // ... 跟之前的校验逻辑保持一致
//      2.判断 url 是否合法
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")){
            new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址不合法,只接受HTTP和HTTPS");
        }
        HttpResponse response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
        if (!response.isOk()){
            return;
        }
//      3.校验文件类型
        try {
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotEmpty(contentType)){
                final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
//      3.校验文件大小
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotEmpty(contentLength)){
                final long ONE_M = 1024 * 1024L;
                ThrowUtils.throwIf(Long.parseLong(contentLength) > 10 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 10M");
            }
        } catch (Exception e) {
            log.error("图片校验失败", e);
        }finally {
            response.close();
        }
    }  
  
    @Override  
    protected String getOriginFilename(Object inputSource) {  
        String fileUrl = (String) inputSource;  
        // 从 URL 中提取文件名  
        return FileUtil.mainName(fileUrl);
    }  
  
    @Override  
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;  
        // 下载文件到临时目录  
        HttpUtil.downloadFile(fileUrl, file);
    }  
}
