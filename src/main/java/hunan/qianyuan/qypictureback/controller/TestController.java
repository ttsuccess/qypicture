package hunan.qianyuan.qypictureback.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import hunan.qianyuan.qypictureback.annotation.AuthCheck;
import hunan.qianyuan.qypictureback.common.BaseResponse;
import hunan.qianyuan.qypictureback.common.ResultUtils;
import hunan.qianyuan.qypictureback.config.CosClientConfig;
import hunan.qianyuan.qypictureback.exception.BusinessException;
import hunan.qianyuan.qypictureback.exception.ErrorCode;
import hunan.qianyuan.qypictureback.manager.CosManager;
import hunan.qianyuan.qypictureback.manager.FileManager;
import hunan.qianyuan.qypictureback.model.constant.UserConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/")
@Slf4j
public class TestController {
    @Resource
    private CosManager cosManager;
    @Resource
    private FileManager fileManager;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

    /**
     * 测试上传接口测试
     */
    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestParam("file") MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        String formatted = String.format("/test/%s", filename);
        File file = null;
        try {
            file = File.createTempFile(filename, null);
            multipartFile.transferTo(file);
            cosManager.putObject(formatted, file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResultUtils.success(filename);
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @GetMapping("download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

    @PostMapping("/uploadURL")
    public BaseResponse<String> uploadUrl(String url, String uploadPathPrefix) {
        fileManager.uploadPictureByUrl(url, uploadPathPrefix);
        return ResultUtils.success(url);
    }

}
