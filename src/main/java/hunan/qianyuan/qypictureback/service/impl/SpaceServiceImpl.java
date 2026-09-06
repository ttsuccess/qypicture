package hunan.qianyuan.qypictureback.service.impl;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import hunan.qianyuan.qypictureback.exception.BusinessException;
import hunan.qianyuan.qypictureback.exception.ErrorCode;
import hunan.qianyuan.qypictureback.exception.ThrowUtils;
import hunan.qianyuan.qypictureback.mapper.SpaceMapper;
import hunan.qianyuan.qypictureback.domain.dto.space.SpaceAddRequest;
import hunan.qianyuan.qypictureback.domain.dto.space.SpaceQueryRequest;
import hunan.qianyuan.qypictureback.domain.entity.Space;
import hunan.qianyuan.qypictureback.domain.entity.User;
import hunan.qianyuan.qypictureback.enums.SpaceLevelEnum;
import hunan.qianyuan.qypictureback.domain.vo.SpaceVO;
import hunan.qianyuan.qypictureback.service.ISpaceService;
import hunan.qianyuan.qypictureback.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * <p>
 * 空间 服务实现类
 * </p>
 *
 * @author qianyaun
 * @since 2025-08-05
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements ISpaceService {
    @Resource
    private IUserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override

    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    @Override

    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
//      取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(enumByValue.getText()), "spaceLevel", enumByValue.getText());
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // 排序条件
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        }
        return queryWrapper;
    }


    @Override
    public SpaceVO getspaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
//        关联用户信息
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            spaceVO.setUser(userService.getUserVO(loginUser));
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getspaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        Set<Long> userIdSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userIdSet.stream().map(userId -> userService.getById(userId)).collect(Collectors.groupingBy(User::getId));
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }
    @Override
    public void fillSpaceBySpaceLevel(Space space){
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null){
            if (space.getMaxSize()== null){
                space.setMaxSize(spaceLevelEnum.getMaxSize());
            }
            if (space.getMaxCount()== null){
                space.setMaxCount(spaceLevelEnum.getMaxCount());
            }
        }

    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
//        1.填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        if (spaceName == null){
            space.setSpaceName("默认空间");
        }
        if (spaceLevel == null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        fillSpaceBySpaceLevel(space);
//        2.校验参数
        validSpace(space, true);
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        space.setUserId(userId);
//        3.校验权限，非管理员只能创建普通级别的空间
        if (!userService.isAdmin(loginUser) && space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作");
        }
//        4.控制同一用户只能创建一个私有空间
        String lock = String.valueOf(userId).intern();
        long newSpaceId;
        synchronized (lock){
            newSpaceId = transactionTemplate.execute(status -> {
                boolean result = lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(result, ErrorCode.OPERATION_ERROR, "一个用户只能创建一个私有空间");
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "数据库创建失败");
                return space.getId();
            });
        }
        return newSpaceId;
    }
}
