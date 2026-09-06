package hunan.qianyuan.qypictureback.service.impl;

import cn.hutool.core.collection.CollUtil;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import hunan.qianyuan.qypictureback.exception.BusinessException;
import hunan.qianyuan.qypictureback.exception.ErrorCode;
import hunan.qianyuan.qypictureback.exception.ThrowUtils;
import hunan.qianyuan.qypictureback.manager.CosManager;
import hunan.qianyuan.qypictureback.manager.FileManager;
import hunan.qianyuan.qypictureback.manager.upload.FilePictureUpload;
import hunan.qianyuan.qypictureback.manager.upload.PictureUploadTemplate;
import hunan.qianyuan.qypictureback.manager.upload.UrlPictureUpload;
import hunan.qianyuan.qypictureback.mapper.PictureMapper;
import hunan.qianyuan.qypictureback.domain.dto.file.PictureUploadRequest;
import hunan.qianyuan.qypictureback.domain.dto.picture.*;
import hunan.qianyuan.qypictureback.domain.entity.Picture;
import hunan.qianyuan.qypictureback.domain.entity.User;
import hunan.qianyuan.qypictureback.enums.PictureReviewStatusEnum;
import hunan.qianyuan.qypictureback.domain.vo.PictureVO;
import hunan.qianyuan.qypictureback.service.IUserService;
import hunan.qianyuan.qypictureback.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static hunan.qianyuan.qypictureback.configuration.CaffeConfig.LOCAL_CACHE;

/**
 * <p>
 * 图片 服务实现类
 * </p>
 *
 * @author qianyaun
 * @since 2025-07-25
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
    @Resource
    private FileManager fileManager;
    @Resource
    private IUserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
   @Resource
   private UrlPictureUpload urlPictureUpload;
   @Resource
   private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CosManager cosManager;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate pictureUpload = filePictureUpload;
        if (inputSource instanceof String){
            pictureUpload = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUpload.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        List<String> tags = pictureUploadRequest.getTags();
        if (pictureUploadRequest!=null && CollUtil.isNotEmpty(pictureUploadRequest.getTags())){
            tags =  pictureUploadRequest.getTags();
        }
        picture.setTags(JSONUtil.toJsonStr(tags));
        String Category = pictureUploadRequest.getCategory();
        if (pictureUploadRequest!=null && StrUtil.isEmpty(Category)){
            Category =  pictureUploadRequest.getCategory();
        }
        picture.setCategory(JSONUtil.toJsonStr(Category));
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //补充审核参数
        fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }



    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        // 新增：获取搜索文本
        String searchText = pictureQueryRequest.getSearchText();
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // 新增：综合搜索逻辑
        if (StrUtil.isNotBlank(searchText)) {
            // 去除首尾空格
            String text = searchText.trim();
            // 构建OR条件组：匹配名称、描述或标签
            queryWrapper.and(qw -> qw
                    // 匹配名称（模糊查询）
                    .like("name", text)
                    // 匹配描述（模糊查询）
                    .or().like("introduction", text)

            );
        }
        // 标签查询（修复JSON格式错误）
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                if (StrUtil.isNotBlank(tag)) {
                    // 关键修复：使用JSON数组格式 ["标签值"] 而非对象格式
                    // 注意转义字符的正确使用：\" 表示双引号
                    queryWrapper.and(qw -> qw.apply("JSON_CONTAINS(tags, '[\"" + tag + "\"]')"));
                }
            }
        }

        // 排序条件
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        }


        return queryWrapper;
    }
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
//         1.对象转对象分装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
//        2.关联用户信息
        Long userId = picture.getUserId();
        User user = userService.getById(userId);
        pictureVO.setUser(userService.getUserVO(user));
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
//        1.参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id <= 0 || pictureReviewStatusEnum == null){
            new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        2.数据查询
        Picture OldPicture = this.getById(id);
        ThrowUtils.throwIf(OldPicture == null, ErrorCode.NOT_FOUND_ERROR,"图片不存在");
//        3.更新审核状态
        Picture newPicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, newPicture);
        newPicture.setReviewerId(loginUser.getId());
        newPicture.setReviewTime(new Date());
        boolean result = this.updateById(newPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return result;
    }
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        Integer start = pictureUploadByBatchRequest.getStart();
        if (start == null || start < 1) {
            start = 1;
        }

        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "请指定有效数量");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");

        String encodedSearchText = null;
        try {
            encodedSearchText = URLEncoder.encode(searchText, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.error("搜索关键词编码失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索关键词无效");
        }

        String fetchUrl = String.format(
                "https://haowallpaper.com/homeView?page=%s&search=%s",
                start,
                encodedSearchText
        );

        Document document;
        try {
            document = Jsoup.connect(fetchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();
        } catch (IOException e) {
            log.error("获取图片列表页面失败，URL:{}", fetchUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }

        Element container = document.getElementsByClass("main-content").first();
        System.out.println("container: "+ container);
        if (ObjUtil.isNull(container)) {
            log.warn("未找到图片容器，URL:{}", fetchUrl);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到图片数据");
        }

        // 改进选择器
        Elements imgElementList = container.select("img"); // 假设图片是< img >标签，实际请根据页面结构调整
        if (imgElementList.isEmpty()) {
            log.warn("当前页面无图片数据，URL:{}", fetchUrl);
            return 0;
        }

        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            if (uploadCount >= count) {
                break;
            }

            String fileUrl = imgElement.attr("data-src");
            if (StrUtil.isBlank(fileUrl)) {
                fileUrl = imgElement.attr("src");
            }

            if (StrUtil.isBlank(fileUrl) || !fileUrl.startsWith("http")) {
                log.info("跳过无效图片URL: {}", fileUrl);
                continue;
            }

            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            PictureUploadRequest uploadRequest = new PictureUploadRequest();
            uploadRequest.setPicName(String.format("%s--%s--%d", new Date(), namePrefix, uploadCount + 1));
            uploadRequest.setCategory("模板");
            uploadRequest.setTags(Arrays.asList("背景", "高清"));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, uploadRequest, loginUser);
                log.info("图片上传成功，id={}，URL={}", pictureVO.getId(), fileUrl);
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败，URL={}", fileUrl, e);
                continue;
            }
        }

        log.info("批量上传完成，搜索关键词:{}，起始位置:{}，目标数量:{}，实际成功:{}",
                searchText, start, count, uploadCount);
        return uploadCount;
    }


    @Override
        public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
            // 构建缓存 key
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String cacheKey = "yupicture:listPictureVOByPage:" + hashKey;
            Page<PictureVO> cachedPage = null;

           // 1. 查询本地缓存（Caffeine）
            String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
            if (cachedValue != null) {
                cachedPage = JSONUtil.toBean(cachedValue, Page.class);
                return cachedPage;
            }
            // 2. 查询分布式缓存（Redis）
            ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
            cachedValue = valueOps.get(cacheKey);
            if (cachedValue != null) {
                // 如果命中 Redis，存入本地缓存并返回
                LOCAL_CACHE.put(cacheKey, cachedValue);
                cachedPage = JSONUtil.toBean(cachedValue, Page.class);
                return cachedPage;
            }
            // 3. 查询数据库
            Page<Picture> picturePage = this.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                    this.getQueryWrapper(pictureQueryRequest));
            Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
           // 4. 更新缓存
            String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
           // 更新本地缓存
            LOCAL_CACHE.put(cacheKey, cacheValue);
           // 更新 Redis 缓存，设置过期时间为 5 分钟
            long outTime = 300+RandomUtil.randomInt(0, 100);
            valueOps.set(cacheKey, cacheValue, outTime, TimeUnit.SECONDS);
         log.info("缓存命中，缓存 key:{}，缓存 value:{}", cacheKey, cacheValue);
          return pictureVOPage;
        }

    @Override
    @Async
    public void deletePicture(Picture picture) {
//        如果删除的图片被多条记录引用，则不能删除
         Long  count = this.lambdaQuery().eq(Picture::getUrl, picture.getUrl()).count();
         if (count > 1) {
             throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片被引用，不能删除");
         }
         cosManager.delete(picture.getUrl());
        String thumbnailUrl = picture.getThumbnailUrl();
        if (thumbnailUrl != null) {
            cosManager.delete(thumbnailUrl);
        }
    }


}
