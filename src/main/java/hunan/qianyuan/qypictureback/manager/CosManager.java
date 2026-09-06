package hunan.qianyuan.qypictureback.manager;
import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COS;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import hunan.qianyuan.qypictureback.configuration.CosClientConfig;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private COSClient cosClient;
  
    // ... 一些操作 COS 的方法
    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }
    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules =  new ArrayList<>();
        String ObjectKey =FileUtil.mainName(key)+".webp";
        PicOperations.Rule compressRute = new PicOperations.Rule();
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        //      将图片压缩（Webp）
        compressRute.setFileId(ObjectKey);
        compressRute.setBucket(cosClientConfig.getBucket());
        compressRute.setRule("imageMogr2/format/webp");
        rules.add(compressRute);
        //缩略图配置
        if (file.length()>3*1024){
            String thumbnailObjectKey = FileUtil.mainName(key)+"_thumbnail"+".webp";
            thumbnailRule.setFileId(thumbnailObjectKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 512, 512));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 删除对象
     *
     * @param key  唯一键
     */
    public void delete(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
    /**
     * 批量删除对象
     *
     * @param keys 要删除的对象键列表
     * @return 批量删除结果
     * @throws MultiObjectDeleteException 部分删除成功时抛出
     * @throws CosClientException 客户端异常
     * @throws CosServiceException 服务端异常
     */
    public DeleteObjectsResult deleteObjects(List<String> keys)
            throws MultiObjectDeleteException, CosClientException, CosServiceException {

        // 验证参数
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("删除的对象列表不能为空");
        }

        // 检查批量删除数量限制
        if (keys.size() > 1000) {
            throw new IllegalArgumentException("单次批量删除最多支持1000个对象");
        }

        // 创建删除请求
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(cosClientConfig.getBucket());

        // 处理并添加要删除的对象键
        List<DeleteObjectsRequest.KeyVersion> keyList = new ArrayList<>(keys.size());
        for (String key : keys) {
            // 清除首尾的斜线，避免删除失败
            String cleanedKey = key.trim()
                    .replaceAll("^[/\\\\]+", "")
                    .replaceAll("[/\\\\]+$", "");

            if (cleanedKey.isEmpty()) {
                throw new IllegalArgumentException("无效的对象键: " + key);
            }

            keyList.add(new DeleteObjectsRequest.KeyVersion(cleanedKey));
        }
        deleteRequest.setKeys(keyList);

        // 执行删除操作
        DeleteObjectsResult deleteResult;
        try {
            deleteResult = cosClient.deleteObjects(deleteRequest);
        } catch (MultiObjectDeleteException e) {
            // 部分删除成功时，可根据业务需求处理
            logPartialErrors(e);
            throw e; // 继续抛出异常让上层处理
        }

        return deleteResult;
    }

    /**
     * 记录部分删除失败的错误信息
     */
    private void logPartialErrors(MultiObjectDeleteException e) {
        // 这里可以替换为日志框架记录
        System.err.println("部分对象删除失败:");
        for (MultiObjectDeleteException.DeleteError error : e.getErrors()) {
            System.err.printf("对象: %s, 错误码: %s, 错误信息: %s%n",
                    error.getKey(), error.getCode(), error.getMessage());
        }
    }
}
