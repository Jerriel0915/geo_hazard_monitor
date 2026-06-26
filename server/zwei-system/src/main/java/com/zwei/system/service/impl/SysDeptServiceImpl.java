package com.zwei.system.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zwei.common.annotation.DataScope;
import com.zwei.common.constant.UserConstants;
import com.zwei.common.core.domain.TreeSelect;
import com.zwei.common.core.domain.entity.SysDept;
import com.zwei.common.core.domain.entity.SysRole;
import com.zwei.common.core.text.Convert;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.spring.SpringUtils;
import com.zwei.system.mapper.SysDeptMapper;
import com.zwei.system.mapper.SysRoleMapper;
import com.zwei.system.service.ISysDeptService;

/**
 * 部门管理 服务实现
 * 
 * @author zwei
 */
@Service
public class SysDeptServiceImpl implements ISysDeptService
{
    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 查询部门管理数据
     * 
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    @DataScope(deptAlias = "d")
    public List<SysDept> selectDeptList(SysDept dept)
    {
        return deptMapper.selectDeptList(dept);
    }

    /**
     * 查询部门树结构信息
     * 
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(SysDept dept)
    {
        List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 构建前端所需要树结构
     * 
     * @param depts 部门列表
     * @return 树结构列表
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts)
    {
        List<SysDept> returnList = new ArrayList<SysDept>();
        List<Long> tempList = depts.stream().map(SysDept::getDeptId).collect(Collectors.toList());
        for (SysDept dept : depts)
        {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId()))
            {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = depts;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts)
    {
        List<SysDept> deptTrees = buildDeptTree(depts);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     * 
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId)
    {
        SysRole role = roleMapper.selectRoleById(roleId);
        return deptMapper.selectDeptListByRoleId(roleId, role.isDeptCheckStrictly());
    }

    /**
     * 根据部门ID查询信息
     * 
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Override
    public SysDept selectDeptById(Long deptId)
    {
        return deptMapper.selectDeptById(deptId);
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     * 
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId)
    {
        return deptMapper.selectNormalChildrenDeptById(deptId);
    }

    /**
     * 是否存在子节点
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId)
    {
        int result = deptMapper.hasChildByDeptId(deptId);
        return result > 0;
    }

    /**
     * 查询部门是否存在用户
     * 
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId)
    {
        int result = deptMapper.checkDeptExistUser(deptId);
        return result > 0;
    }

    /**
     * 校验部门编码是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptCodeUnique(SysDept dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = deptMapper.checkDeptCodeUnique(dept.getCode());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验部门名称是否唯一
     * 
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDept dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = deptMapper.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验部门是否有数据权限
     * 
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId)
    {
        if (!SecurityUtils.isAdmin() && StringUtils.isNotNull(deptId))
        {
            SysDept dept = new SysDept();
            dept.setDeptId(deptId);
            List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
            if (StringUtils.isEmpty(depts))
            {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * 新增保存部门信息
     * 
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int insertDept(SysDept dept)
    {
        SysDept info = null;
        if (dept.getParentId() != null && dept.getParentId() != 0L)
        {
            info = deptMapper.selectDeptById(dept.getParentId());
        }
        if (info != null && !UserConstants.DEPT_NORMAL.equals(info.getStatus()))
        {
            throw new ServiceException("部门停用，不允许新增");
        }
        populateHierarchyFields(dept, info);
        return deptMapper.insertDept(dept);
    }

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDept(SysDept dept)
    {
        SysDept oldDept = deptMapper.selectDeptById(dept.getDeptId());
        SysDept newParentDept = null;
        if (dept.getParentId() != null && dept.getParentId() != 0L)
        {
            newParentDept = deptMapper.selectDeptById(dept.getParentId());
            if (StringUtils.isNotNull(newParentDept) && !UserConstants.DEPT_NORMAL.equals(newParentDept.getStatus()))
            {
                throw new ServiceException("部门停用，不允许修改");
            }
        }
        if (StringUtils.isNotNull(oldDept))
        {
            String newAncestors = buildAncestors(newParentDept);
            String oldAncestors = oldDept.getAncestors();
            String newParentIds = buildParentIds(newParentDept);
            String oldParentIds = oldDept.getParentIds();
            int newLevel = buildLevel(newParentDept);
            dept.setAncestors(newAncestors);
            dept.setParentIds(newParentIds);
            dept.setLevel(newLevel);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors, newParentIds, oldParentIds, newLevel);
        }
        int result = deptMapper.updateDept(dept);
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus()) && StringUtils.isNotEmpty(dept.getAncestors())
                && !StringUtils.equals("0", dept.getAncestors()))
        {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    /**
     * 修改该部门的父级部门状态
     * 
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(SysDept dept)
    {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        deptMapper.updateDeptStatusNormal(deptIds);
    }

    /**
     * 修改子元素关系
     * 
     * @param deptId 被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    public void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors, String newParentIds, String oldParentIds, int newLevel)
    {
        List<SysDept> children = deptMapper.selectChildrenDeptById(deptId);
        for (SysDept child : children)
        {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            if (StringUtils.isNotEmpty(oldParentIds) && StringUtils.isNotEmpty(newParentIds) && StringUtils.isNotEmpty(child.getParentIds()))
            {
                child.setParentIds(child.getParentIds().replaceFirst(oldParentIds, newParentIds));
            }
            int relativeLevel = child.getLevel() == null || child.getLevel() <= 0 || oldDeptLevel(oldAncestors) <= 0
                    ? 1
                    : child.getLevel() - oldDeptLevel(oldAncestors);
            child.setLevel(newLevel + relativeLevel);
        }
        if (children.size() > 0)
        {
            deptMapper.updateDeptChildren(children);
        }
    }

    /**
     * 保存部门排序
     *
     * @param deptIds 部门ID数组
     * @param orderNums 排序数组
     */
    @Override
    @Transactional
    public void updateDeptSort(String[] deptIds, String[] orderNums)
    {
        try
        {
            for (int i = 0; i < deptIds.length; i++)
            {
                SysDept dept = new SysDept();
                dept.setDeptId(Convert.toLong(deptIds[i]));
                dept.setOrderNum(Convert.toInt(orderNums[i]));
                deptMapper.updateDeptSort(dept);
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("保存排序异常，请联系管理员");
        }
    }

    /**
     * 删除部门管理信息
     * 
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public int deleteDeptById(Long deptId)
    {
        return deptMapper.deleteDeptById(deptId);
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysDept> list, SysDept t)
    {
        // 得到子节点列表
        List<SysDept> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysDept tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysDept> getChildList(List<SysDept> list, SysDept t)
    {
        List<SysDept> tlist = new ArrayList<SysDept>();
        Iterator<SysDept> it = list.iterator();
        while (it.hasNext())
        {
            SysDept n = (SysDept) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysDept> list, SysDept t)
    {
        return getChildList(list, t).size() > 0;
    }

    private void populateHierarchyFields(SysDept dept, SysDept parentDept)
    {
        dept.setAncestors(buildAncestors(parentDept));
        dept.setParentIds(buildParentIds(parentDept));
        dept.setLevel(buildLevel(parentDept));
    }

    private String buildAncestors(SysDept parentDept)
    {
        if (parentDept == null)
        {
            return "0";
        }
        return parentDept.getAncestors() + "," + parentDept.getDeptId();
    }

    private String buildParentIds(SysDept parentDept)
    {
        if (parentDept == null)
        {
            return "/0/";
        }
        String parentIds = StringUtils.isNotEmpty(parentDept.getParentIds()) ? parentDept.getParentIds() : "/0/";
        return parentIds + parentDept.getDeptId() + "/";
    }

    private int buildLevel(SysDept parentDept)
    {
        if (parentDept == null || parentDept.getLevel() == null)
        {
            return 1;
        }
        return parentDept.getLevel() + 1;
    }

    private int oldDeptLevel(String oldAncestors)
    {
        if (StringUtils.isEmpty(oldAncestors))
        {
            return 0;
        }
        return oldAncestors.split(",").length + 1;
    }
}
