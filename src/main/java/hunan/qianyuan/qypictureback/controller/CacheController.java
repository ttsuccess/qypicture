package hunan.qianyuan.qypictureback.controller;

import hunan.qianyuan.qypictureback.common.BaseResponse;
import hunan.qianyuan.qypictureback.common.ResultUtils;
import hunan.qianyuan.qypictureback.exception.BusinessException;
import hunan.qianyuan.qypictureback.exception.ErrorCode;
import hunan.qianyuan.qypictureback.service.CacheService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 缓存管理控制器（仅管理员可访问）
 */
@RestController
@RequestMapping("/cache")
public class CacheController {

    @Resource
    private CacheService cacheService;

    /**
     * 刷新所有缓存
     * @param request 请求对象（用于权限校验）
     * @return 刷新结果
     */
    @PostMapping("/refreshAll")
    public BaseResponse<Boolean> refreshAllCache(HttpServletRequest request) {
        // 执行缓存刷新
        boolean result = cacheService.refreshAllCache(request);
        return ResultUtils.success(result);
    }

    /**
     * 刷新指定类型的缓存
     * @param cacheType 缓存类型（如"picture"、"user"等）
     * @param request 请求对象（用于权限校验）
     * @return 刷新结果
     */
    @PostMapping("/refreshByType")
    public BaseResponse<Boolean> refreshCacheByType(String cacheType, HttpServletRequest request) {
        if (cacheType == null || cacheType.trim().isEmpty()) {
          throw new BusinessException(ErrorCode.PARAMS_ERROR, "缓存类型不能为空");
        }
        boolean result = cacheService.refreshCacheByType(cacheType.trim(), request);
        return ResultUtils.success(result);
    }
}
    