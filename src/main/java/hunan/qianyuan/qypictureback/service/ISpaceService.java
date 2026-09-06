package hunan.qianyuan.qypictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import hunan.qianyuan.qypictureback.domain.dto.space.SpaceAddRequest;
import hunan.qianyuan.qypictureback.domain.dto.space.SpaceQueryRequest;
import hunan.qianyuan.qypictureback.domain.entity.Space;
import hunan.qianyuan.qypictureback.domain.vo.SpaceVO;


import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 空间 服务类
 * </p>
 *
 * @author qianyaun
 * @since 2025-08-05
 */
public interface ISpaceService extends IService<Space> {
    /**
     * 图片校验
     * @param space
     */

    void validSpace(Space space, boolean add);

    /**
     * 查询条件
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取封装的spaceVO
     * @param space
     * @param request
     * @return
     */
    SpaceVO getspaceVO(Space space, HttpServletRequest request);

    /**
     *  分页获取封装的spaceVO
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getspaceVOPage(Page<Space> spacePage , HttpServletRequest request);

    /**
     * 在创建和更新时，填充空间级别信息
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);
}
