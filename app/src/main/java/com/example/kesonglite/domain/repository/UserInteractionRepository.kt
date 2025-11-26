package com.example.kesonglite.domain.repository

/**
 * 用户交互数据仓库接口
 * 定义用户交互相关的抽象方法
 */
interface UserInteractionRepository {
    
    /**
     * 切换帖子点赞状态
     * @param postId 帖子ID
     * @return 新的点赞状态
     */
    suspend fun toggleLike(postId: String): Boolean
    
    /**
     * 获取帖子点赞状态
     * @param postId 帖子ID
     * @return 点赞状态
     */
    suspend fun getLikeState(postId: String): Boolean
    
    /**
     * 更新点赞数量
     * @param postId 帖子ID
     * @param count 点赞数量
     */
    suspend fun updateLikeCount(postId: String, count: Int)
    
    /**
     * 获取点赞数量
     * @param postId 帖子ID
     * @return 点赞数量
     */
    suspend fun getLikeCount(postId: String): Int
    
    /**
     * 切换用户关注状态
     * @param userId 用户ID
     * @return 新的关注状态
     */
    suspend fun toggleFollow(userId: String): Boolean
    
    /**
     * 获取用户关注状态
     * @param userId 用户ID
     * @return 关注状态
     */
    suspend fun getFollowState(userId: String): Boolean
}