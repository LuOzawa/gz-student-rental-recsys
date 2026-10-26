package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.ForumCategory;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ForumCategoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ForumCategoryService {

    @Resource
    private ForumCategoryMapper forumCategoryMapper;

    public List<ForumCategory> getAllCategories() {
        LambdaQueryWrapper<ForumCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ForumCategory::getStatus, 1)
                .orderByAsc(ForumCategory::getSortOrder);
        return forumCategoryMapper.selectList(queryWrapper);
    }

    public ForumCategory getById(Long id) {
        return forumCategoryMapper.selectById(id);
    }

    public ForumCategory createCategory(ForumCategory category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        forumCategoryMapper.insert(category);
        return category;
    }

    public ForumCategory updateCategory(Long id, ForumCategory category) {
        ForumCategory existing = forumCategoryMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("分类不存在");
        }
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        forumCategoryMapper.updateById(category);
        return category;
    }

    public void deleteCategory(Long id) {
        forumCategoryMapper.deleteById(id);
    }
}
