package com.example.anime.service;

import com.example.anime.model.Anime;
import com.example.anime.model.AnimeComment;
import com.example.anime.model.CommentInteraction;
import com.example.anime.model.User;
import com.example.anime.repository.AnimeCommentRepository;
import com.example.anime.repository.AnimeRepository;
import com.example.anime.repository.CommentInteractionRepository;
import com.example.anime.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnimeCommentService {
    @Autowired
    private AnimeCommentRepository animeCommentRepository;
    
    @Autowired
    private CommentInteractionRepository commentInteractionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private AnimeRepository animeRepository;

    // 保存评论
    public AnimeComment saveComment(Long animeId, Long authorId, String content, Long parentId) {
        AnimeComment comment = new AnimeComment();
        comment.setAnimeId(animeId);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment.setCreateTime(new Date());
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        // 测试用户创建的评论自动标记为测试数据
        User author = userService.findById(authorId);
        if (author != null && author.getIsTest() != null && author.getIsTest()) {
            comment.setIsTest(true);
        }

        AnimeComment savedComment = animeCommentRepository.save(comment);

        // 测试用户不发送通知给真实用户
        boolean isTestUser = author != null && author.getIsTest() != null && author.getIsTest();

        // 回复评论时通知被回复用户
        if (parentId != null) {
            AnimeComment parentComment = animeCommentRepository.findById(parentId).orElse(null);
            if (parentComment != null && !parentComment.getAuthorId().equals(authorId)) {
                User targetUser = userService.findById(parentComment.getAuthorId());
                User fromUser = userService.findById(authorId);
                Anime anime = animeRepository.findById(animeId).orElse(null);
                if (targetUser != null && fromUser != null && anime != null && !isTestUser) {
                    String animeTitle = anime.getTitle() != null ? anime.getTitle() : "未知动漫";
                    notificationService.notifyAnimeReply(
                            targetUser.getId(),
                            targetUser.getUsername(),
                            fromUser.getUsername(),
                            animeTitle,
                            content,
                            animeId,
                            savedComment.getId()
                    );
                }
            }
        }

        return savedComment;
    }

    // 保存评论实体
    public AnimeComment save(AnimeComment comment) {
        return animeCommentRepository.save(comment);
    }

    // 过滤测试评论
    private List<AnimeComment> filterTestComments(List<AnimeComment> comments) {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return comments;
        }
        return comments.stream()
                .filter(c -> c.getIsTest() == null || !c.getIsTest())
                .collect(Collectors.toList());
    }

    // 获取动漫的所有顶级评论（不包含回复）
    public List<AnimeComment> getAnimeComments(Long animeId) {
        return filterTestComments(animeCommentRepository.findByAnimeIdAndParentIdIsNullOrderByCreateTimeDesc(animeId));
    }

    // 获取评论的子评论
    public List<AnimeComment> getCommentReplies(Long animeId, Long parentId) {
        return filterTestComments(animeCommentRepository.findByAnimeIdAndParentIdOrderByCreateTimeAsc(animeId, parentId));
    }

    // 点赞评论
    public AnimeComment likeComment(Long commentId, Long userId) {
        boolean likeAdded = false;
        // 检查用户是否已经对该评论进行过互动
        Optional<CommentInteraction> existingInteraction = commentInteractionRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingInteraction.isPresent()) {
            // 如果已经是点赞，则取消点赞
            if (existingInteraction.get().getInteractionType() == 1) {
                commentInteractionRepository.delete(existingInteraction.get());
            } else {
                // 如果是点踩，则先删除点踩，再添加点赞
                commentInteractionRepository.delete(existingInteraction.get());
                CommentInteraction interaction = new CommentInteraction();
                interaction.setCommentId(commentId);
                interaction.setUserId(userId);
                interaction.setInteractionType(1); // 1: 点赞
                interaction.setCreateTime(new Date());
                commentInteractionRepository.save(interaction);
                likeAdded = true;
            }
        } else {
            // 没有互动过，添加点赞
            CommentInteraction interaction = new CommentInteraction();
            interaction.setCommentId(commentId);
            interaction.setUserId(userId);
            interaction.setInteractionType(1); // 1: 点赞
            interaction.setCreateTime(new Date());
            commentInteractionRepository.save(interaction);
            likeAdded = true;
        }
        
        Optional<AnimeComment> commentOptional = animeCommentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            // 重新计算点赞数和点踩数
            AnimeComment comment = commentOptional.get();
            comment.setLikeCount(commentInteractionRepository.countByCommentIdAndInteractionType(commentId, 1));
            comment.setDislikeCount(commentInteractionRepository.countByCommentIdAndInteractionType(commentId, 2));
            AnimeComment savedComment = animeCommentRepository.save(comment);

            // 新增点赞时通知评论作者（不通知自己）
            if (likeAdded && !comment.getAuthorId().equals(userId)) {
                User targetUser = userService.findById(comment.getAuthorId());
                User fromUser = userService.findById(userId);
                // 测试用户不发送通知
                if (fromUser != null && fromUser.getIsTest() != null && fromUser.getIsTest()) {
                    return savedComment;
                }
                Anime anime = animeRepository.findById(comment.getAnimeId()).orElse(null);
                if (targetUser != null && fromUser != null && anime != null) {
                    String animeTitle = anime.getTitle() != null ? anime.getTitle() : "未知动漫";
                    notificationService.notifyAnimeLike(
                            targetUser.getId(),
                            targetUser.getUsername(),
                            fromUser.getUsername(),
                            animeTitle,
                            comment.getAnimeId(),
                            commentId
                    );
                }
            }

            return savedComment;
        }
        return null;
    }

    // 点踩评论
    public AnimeComment dislikeComment(Long commentId, Long userId) {
        boolean dislikeAdded = false;
        // 检查用户是否已经对该评论进行过互动
        Optional<CommentInteraction> existingInteraction = commentInteractionRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingInteraction.isPresent()) {
            // 如果已经是点踩，则取消点踩
            if (existingInteraction.get().getInteractionType() == 2) {
                commentInteractionRepository.delete(existingInteraction.get());
            } else {
                // 如果是点赞，则先删除点赞，再添加点踩
                commentInteractionRepository.delete(existingInteraction.get());
                CommentInteraction interaction = new CommentInteraction();
                interaction.setCommentId(commentId);
                interaction.setUserId(userId);
                interaction.setInteractionType(2); // 2: 点踩
                interaction.setCreateTime(new Date());
                commentInteractionRepository.save(interaction);
                dislikeAdded = true;
            }
        } else {
            // 没有互动过，添加点踩
            CommentInteraction interaction = new CommentInteraction();
            interaction.setCommentId(commentId);
            interaction.setUserId(userId);
            interaction.setInteractionType(2); // 2: 点踩
            interaction.setCreateTime(new Date());
            commentInteractionRepository.save(interaction);
            dislikeAdded = true;
        }
        
        Optional<AnimeComment> commentOptional = animeCommentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            // 重新计算点赞数和点踩数
            AnimeComment comment = commentOptional.get();
            comment.setLikeCount(commentInteractionRepository.countByCommentIdAndInteractionType(commentId, 1));
            comment.setDislikeCount(commentInteractionRepository.countByCommentIdAndInteractionType(commentId, 2));
            AnimeComment savedComment = animeCommentRepository.save(comment);

            // 新增点踩时通知评论作者（不通知自己）
            if (dislikeAdded && !comment.getAuthorId().equals(userId)) {
                User targetUser = userService.findById(comment.getAuthorId());
                User fromUser = userService.findById(userId);
                // 测试用户不发送通知
                if (fromUser != null && fromUser.getIsTest() != null && fromUser.getIsTest()) {
                    return savedComment;
                }
                Anime anime = animeRepository.findById(comment.getAnimeId()).orElse(null);
                if (targetUser != null && fromUser != null && anime != null) {
                    String animeTitle = anime.getTitle() != null ? anime.getTitle() : "未知动漫";
                    notificationService.notifyAnimeDislike(
                            targetUser.getId(),
                            targetUser.getUsername(),
                            fromUser.getUsername(),
                            animeTitle,
                            comment.getAnimeId(),
                            commentId
                    );
                }
            }

            return savedComment;
        }
        return null;
    }

    // 根据ID删除评论（用于管理员）
    public void deleteById(Long commentId) {
        deleteComment(commentId);
    }

    // 删除评论（物理删除，包括回复）
    @javax.transaction.Transactional
    public void deleteComment(Long commentId) {
        Optional<AnimeComment> commentOptional = animeCommentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            AnimeComment comment = commentOptional.get();
            
            // 递归物理删除所有回复
            deleteReplies(comment.getAnimeId(), commentId);
            
            // 先删除主评论的互动记录
            List<CommentInteraction> interactions = commentInteractionRepository.findByCommentId(commentId);
            for (CommentInteraction interaction : interactions) {
                commentInteractionRepository.delete(interaction);
            }
            
            // 删除与该评论相关的通知
            notificationService.deleteByCommentId(commentId);
            
            // 再物理删除主评论
            animeCommentRepository.deleteById(commentId);
        }
    }
    
    // 递归物理删除所有回复
    private void deleteReplies(Long animeId, Long commentId) {
        List<AnimeComment> replies = animeCommentRepository.findByAnimeIdAndParentIdOrderByCreateTimeAsc(animeId, commentId);
        for (AnimeComment reply : replies) {
            // 先递归删除回复的回复
            deleteReplies(animeId, reply.getId());
            
            // 再删除回复的互动记录
            List<CommentInteraction> replyInteractions = commentInteractionRepository.findByCommentId(reply.getId());
            for (CommentInteraction interaction : replyInteractions) {
                commentInteractionRepository.delete(interaction);
            }
            
            // 删除与该回复相关的通知
            notificationService.deleteByCommentId(reply.getId());
            
            // 最后物理删除回复
            animeCommentRepository.deleteById(reply.getId());
        }
    }
    
    // 根据用户ID获取评论（先返回回复，再返回顶级评论，避免外键约束冲突）
    public List<AnimeComment> getCommentsByAuthorId(Long authorId) {
        List<AnimeComment> allComments = animeCommentRepository.findByAuthorIdOrderByCreateTimeDesc(authorId);
        // 先返回回复（parent_id不为null），再返回顶级评论（parent_id为null）
        List<AnimeComment> replies = allComments.stream()
            .filter(comment -> comment.getParentId() != null)
            .collect(java.util.stream.Collectors.toList());
        List<AnimeComment> topLevelComments = allComments.stream()
            .filter(comment -> comment.getParentId() == null)
            .collect(java.util.stream.Collectors.toList());
        replies.addAll(topLevelComments);
        return filterTestComments(replies);
    }
    
    // 获取所有评论
    public List<AnimeComment> getAllComments() {
        return filterTestComments(animeCommentRepository.findAll());
    }
    
    // 删除用户的所有评论
    @javax.transaction.Transactional
    public void deleteByAuthorId(Long authorId) {
        try {
            log.debug("开始删除用户的所有评论，用户ID: " + authorId);
            
            // 1. 获取用户的所有评论
            List<AnimeComment> userComments = animeCommentRepository.findByAuthorIdOrderByCreateTimeDesc(authorId);
            log.debug("找到 " + userComments.size() + " 条评论");
            
            // 2. 收集所有评论ID
            java.util.List<Long> commentIds = new java.util.ArrayList<>();
            for (AnimeComment comment : userComments) {
                commentIds.add(comment.getId());
            }
            
            // 3. 删除这些评论的互动记录
            log.debug("删除用户评论的互动记录...");
            for (Long commentId : commentIds) {
                List<CommentInteraction> interactions = commentInteractionRepository.findByCommentId(commentId);
                for (CommentInteraction interaction : interactions) {
                    commentInteractionRepository.delete(interaction);
                }
            }
            
            // 4. 将所有引用用户评论的评论的parent_id设置为null，避免外键约束冲突
            log.debug("将引用用户评论的评论的parent_id设置为null...");
            for (Long commentId : commentIds) {
                animeCommentRepository.updateParentIdToNullByParentId(commentId);
            }
            
            // 5. 然后删除用户的所有评论
            log.debug("删除用户的所有评论...");
            animeCommentRepository.deleteCommentsByAuthorId(authorId);
            
            log.debug("用户评论删除完成，用户ID: " + authorId);
        } catch (Exception e) {
            log.debug("删除用户评论失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    // 根据动漫ID删除所有相关的评论
    @javax.transaction.Transactional
    public void deleteByAnimeId(Long animeId) {
        try {
            log.debug("开始删除动漫的所有评论，动漫ID: " + animeId);
            
            // 1. 获取该动漫的所有评论
            List<AnimeComment> allComments = animeCommentRepository.findByAnimeIdOrderByCreateTimeDesc(animeId);
            log.debug("找到 " + allComments.size() + " 条评论");
            
            // 2. 收集所有评论ID
            java.util.List<Long> commentIds = new java.util.ArrayList<>();
            for (AnimeComment comment : allComments) {
                commentIds.add(comment.getId());
            }
            
            // 3. 删除这些评论的互动记录
            log.debug("删除评论的互动记录...");
            for (Long commentId : commentIds) {
                List<CommentInteraction> interactions = commentInteractionRepository.findByCommentId(commentId);
                for (CommentInteraction interaction : interactions) {
                    commentInteractionRepository.delete(interaction);
                }
            }
            
            // 4. 删除这些评论关联的通知
            log.debug("删除评论关联的通知...");
            for (Long commentId : commentIds) {
                notificationService.deleteByCommentId(commentId);
            }
            // 同时删除该动漫相关的所有通知（如点赞/点踩通知）
            notificationService.deleteByTarget("anime", animeId);
            
            // 5. 将所有引用这些评论的评论的parent_id设置为null，避免外键约束冲突
            log.debug("将引用这些评论的评论的parent_id设置为null...");
            for (Long commentId : commentIds) {
                animeCommentRepository.updateParentIdToNullByParentId(commentId);
            }
            
            // 6. 然后删除所有评论
            log.debug("删除所有评论...");
            animeCommentRepository.deleteByAnimeId(animeId);
            
            log.debug("动漫评论删除完成，动漫ID: " + animeId);
        } catch (Exception e) {
            log.debug("删除动漫评论失败: " + e.getMessage());
            e.printStackTrace();
            // 如果还是失败，记录错误但不抛出异常，确保动漫删除流程继续
        }
    }
}