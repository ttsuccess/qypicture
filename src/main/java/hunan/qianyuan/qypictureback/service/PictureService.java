package hunan.qianyuan.qypictureback.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import hunan.qianyuan.qypictureback.model.dto.file.PictureUploadRequest;
import hunan.qianyuan.qypictureback.model.dto.picture.PictureQueryRequest;
import hunan.qianyuan.qypictureback.model.dto.picture.PictureReviewRequest;
import hunan.qianyuan.qypictureback.model.dto.picture.PictureTagCategory;
import hunan.qianyuan.qypictureback.model.dto.picture.PictureUploadByBatchRequest;
import hunan.qianyuan.qypictureback.model.dto.user.UserQueryRequest;
import hunan.qianyuan.qypictureback.model.entity.Picture;
import hunan.qianyuan.qypictureback.model.entity.User;
import hunan.qianyuan.qypictureback.model.vo.LoginUserVO;
import hunan.qianyuan.qypictureback.model.vo.PictureVO;
import hunan.qianyuan.qypictureback.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author qianyaun
 * @since 2025-07-24
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 查询条件
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取封装的pictureVO
     * @param picture
     * @param request
     * @return
     */
   PictureVO getPictureVO(Picture picture, HttpServletRequest request);

   Page<PictureVO> getPictureVOPage(Page<Picture> picturePage , HttpServletRequest request);

    /**
     * 图片校验
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    ) throws IOException;

    /**
     * 实现多级缓存分页查询
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 清理对象存储中的图片
     * @param picture
     */
    void deletePicture(Picture  picture);
}
