package hunan.qianyuan.qypictureback.service;

import javax.servlet.http.HttpServletRequest;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 刷新所有缓存
     * @param request 请求对象
     * @return 是否刷新成功
     */
    boolean refreshAllCache(HttpServletRequest request);

    /**
     * 按类型刷新缓存
     * @param cacheType 缓存类型
     * @param request 请求对象
     * @return 是否刷新成功
     */
    boolean refreshCacheByType(String cacheType, HttpServletRequest request);
}
    