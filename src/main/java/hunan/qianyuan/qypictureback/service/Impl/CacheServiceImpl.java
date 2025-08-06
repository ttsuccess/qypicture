package hunan.qianyuan.qypictureback.service.Impl;
import cn.hutool.core.collection.CollUtil;
import hunan.qianyuan.qypictureback.config.CaffeConfig;
import hunan.qianyuan.qypictureback.model.entity.User;
import hunan.qianyuan.qypictureback.service.CacheService;
import hunan.qianyuan.qypictureback.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;
/**
 * 缓存服务实现类
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    /**
     * 缓存前缀常量
     */
    private static final String PICTURE_CACHE_PREFIX = "picture:list:withCache:";

    @Override
    public boolean refreshAllCache(HttpServletRequest request) {
        // 1. 校验管理员权限
        if (!isAdmin(request)) {
            log.warn("非管理员尝试刷新所有缓存，IP:{}", getIpAddress(request));
            return false;
        }

        try {
            // 2. 清除本地缓存
            CaffeConfig.LOCAL_CACHE.invalidateAll();
            log.info("本地缓存已全部清除");

            // 3. 清除Redis所有缓存（生产环境谨慎使用！）
            // 注意：生产环境建议按前缀清除，避免影响其他业务缓存
            Set<String> keys = stringRedisTemplate.keys("*");
            if (CollUtil.isNotEmpty(keys)) {
                stringRedisTemplate.delete(keys);
                log.info("Redis缓存已全部清除，共清除{}个键", keys.size());
            } else {
                log.info("Redis中没有缓存数据");
            }

            return true;
        } catch (Exception e) {
            log.error("刷新所有缓存失败", e);
            return false;
        }
    }

    @Override
    public boolean refreshCacheByType(String cacheType, HttpServletRequest request) {
        // 1. 校验管理员权限
        if (!isAdmin(request)) {
            log.warn("非管理员尝试刷新[{}]类型缓存，IP:{}", cacheType, getIpAddress(request));
            return false;
        }

        try {
            // 2. 根据缓存类型清除对应缓存
            switch (cacheType) {
                case "picture":
                    // 清除图片相关本地缓存
                    CaffeConfig.LOCAL_CACHE.asMap().keySet().removeIf(
                            key -> key.toString().startsWith(PICTURE_CACHE_PREFIX)
                    );
                    
                    // 清除Redis中图片相关缓存
                    Set<String> pictureKeys = stringRedisTemplate.keys(PICTURE_CACHE_PREFIX + "*");
                    if (CollUtil.isNotEmpty(pictureKeys)) {
                        stringRedisTemplate.delete(pictureKeys);
                        log.info("已清除图片类型Redis缓存，共{}个键", pictureKeys.size());
                    }
                    log.info("图片类型缓存已刷新");
                    break;
                    
                case "user":
                    // 这里可以添加用户相关缓存的清除逻辑
                    log.info("用户类型缓存已刷新");
                    break;
                    
                default:
                    log.warn("不支持的缓存类型:{}", cacheType);
                    return false;
            }

            return true;
        } catch (Exception e) {
            log.error("刷新[{}]类型缓存失败", cacheType, e);
            return false;
        }
    }

    /**
     * 校验是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return loginUser != null && "admin".equals(loginUser.getUserRole());
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
    