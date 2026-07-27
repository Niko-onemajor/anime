package com.example.anime.service;

import com.example.anime.model.Comment;
import com.example.anime.model.ForumCommentInteraction;
import com.example.anime.model.Post;
import com.example.anime.model.User;
import com.example.anime.repository.CommentRepository;
import com.example.anime.repository.ForumCommentInteractionRepository;
import com.example.anime.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private ForumCommentInteractionRepository forumCommentInteractionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostRepository postRepository;

    // 添加评论
    public Comment addComment(Long postId, Long authorId, String content) {
        return addComment(postId, authorId, content, null);
    }

    // 添加评论或回复
    public Comment addComment(Long postId, Long authorId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreateTime(new Date());
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        Comment savedComment = commentRepository.save(comment);

        // 回复评论时通知被回复用户
        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment != null && !parentComment.getAuthorId().equals(authorId)) {
                User targetUser = userService.findById(parentComment.getAuthorId());
                User fromUser = userService.findById(authorId);
                Post post = postRepository.findById(postId).orElse(null);
                if (targetUser != null && fromUser != null && post != null) {
                    String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                    notificationService.notifyForumReply(
                            targetUser.getId(),
                            targetUser.getUsername(),
                            fromUser.getUsername(),
                            postTitle,
                            content
                    );
                }
            }
        } else {
            // 顶级评论：通知帖子作者
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null && post.getAuthor() != null && !post.getAuthor().getId().equals(authorId)) {
                User fromUser = userService.findById(authorId);
                if (fromUser != null) {
                    String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                    notificationService.notifyForumReply(
                            post.getAuthor().getId(),
                            post.getAuthor().getUsername(),
                            fromUser.getUsername(),
                            postTitle,
                            content
                    );
                }
            }
        }

        return savedComment;
    }

    // 获取帖子的顶级评论（不含回复）
    public List<Comment> getTopLevelComments(Long postId) {
        return commentRepository.findByPostIdAndParentIdIsNull(postId);
    }

    // 获取评论的回复列表
    public List<Comment> getReplies(Long parentId) {
        return commentRepository.findByParentId(parentId);
    }

    // 根据ID查找评论
    public Comment findById(Long id) {
        return commentRepository.findById(id).orElse(null);
    }

    // 根据帖子ID获取评论列表
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }



    // 根据用户ID获取评论列表
    public List<Comment> getCommentsByAuthorId(Long authorId) {
        return commentRepository.findByAuthorId(authorId);
    }

    // 删除评论（级联删除回复）
    public Comment deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            // 先删除所有子回复
            List<Comment> replies = commentRepository.findByParentId(commentId);
            for (Comment reply : replies) {
                deleteComment(reply.getId());
            }
            
            // 删除评论的互动记录
            List<ForumCommentInteraction> interactions = forumCommentInteractionRepository.findAll().stream()
                    .filter(interaction -> interaction.getCommentId().equals(commentId))
                    .collect(java.util.stream.Collectors.toList());
            for (ForumCommentInteraction interaction : interactions) {
                forumCommentInteractionRepository.delete(interaction);
            }
            // 再删除评论
            commentRepository.delete(comment);
        }
        return comment;
    }

    // 根据帖子ID获取评论列表（用于管理员）
    public List<Comment> findByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    // 根据ID删除评论（用于管理员）
    public Comment deleteById(Long commentId) {
        return deleteComment(commentId);
    }
    
    // 更新评论（用于管理员）
    public Comment updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            comment.setContent(content);
            return commentRepository.save(comment);
        }
        return null;
    }

    // 点赞评论
    public Comment likeComment(Long commentId, Long userId) {
        boolean likeAdded = false;
        // 检查用户是否已经对该评论进行过互动
        Optional<ForumCommentInteraction> existingInteraction = forumCommentInteractionRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingInteraction.isPresent()) {
            // 如果已经是点赞，则取消点赞
            if (existingInteraction.get().getInteractionType() == 1) {
                forumCommentInteractionRepository.delete(existingInteraction.get());
            } else {
                // 如果是点踩，则先删除点踩，再添加点赞
                forumCommentInteractionRepository.delete(existingInteraction.get());
                ForumCommentInteraction interaction = new ForumCommentInteraction();
                interaction.setCommentId(commentId);
                interaction.setUserId(userId);
                interaction.setInteractionType(1); // 1: 点赞
                interaction.setCreateTime(new Date());
                forumCommentInteractionRepository.save(interaction);
                likeAdded = true;
            }
        } else {
            // 没有互动过，添加点赞
            ForumCommentInteraction interaction = new ForumCommentInteraction();
            interaction.setCommentId(commentId);
            interaction.setUserId(userId);
            interaction.setInteractionType(1); // 1: 点赞
            interaction.setCreateTime(new Date());
            forumCommentInteractionRepository.save(interaction);
            likeAdded = true;
        }
        
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            // 重新计算点赞数和点踩数
            Comment comment = commentOptional.get();
            comment.setLikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 1));
            comment.setDislikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 2));
            Comment savedComment = commentRepository.save(comment);

            // 新增点赞时通知评论作者（不通知自己）
            if (likeAdded && !comment.getAuthorId().equals(userId)) {
                User targetUser = userService.findById(comment.getAuthorId());
                User fromUser = userService.findById(userId);
                Post post = postRepository.findById(comment.getPostId()).orElse(null);
                if (targetUser != null && fromUser != null && post != null) {
                    String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                    notificationService.notifyForumLike(
                            targetUser.getId(),
                            targetUser.getUsername(),
                            fromUser.getUsername(),
                            postTitle
                    );
                }
            }

            return savedComment;
        }
        return null;
    }

    // 点踩评论
    public Comment dislikeComment(Long commentId, Long userId) {
        boolean dislikeAdded = false;
        // 检查用户是否已经对该评论进行过互动
        Optional<ForumCommentInteraction> existingInteraction = forumCommentInteractionRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingInteraction.isPresent()) {
            // 如果已经是点踩，则取消点踩
            if (existingInteraction.get().getInteractionType() == 2) {
                forumCommentInteractionRepository.delete(existingInteraction.get());
            } else {
                // 如果是点赞，则先删除点赞，再添加点踩
                forumCommentInteractionRepository.delete(existingInteraction.get());
                ForumCommentInteraction interaction = new ForumCommentInteraction();
                interaction.setCommentId(commentId);
                interaction.setUserId(userId);
                interaction.setInteractionType(2); // 2: 点踩
                interaction.setCreateTime(new Date());
                forumCommentInteractionRepository.save(interaction);
                dislikeAdded = true;
            }
        } else {
            // 没有互动过，添加点踩
            ForumCommentInteraction interaction = new ForumCommentInteraction();
            interaction.setCommentId(commentId);
            interaction.setUserId(userId);
            interaction.setInteractionType(2); // 2: 点踩
            interaction.setCreateTime(new Date());
            forumCommentInteractionRepository.save(interaction);
            dislikeAdded = true;
        }
        
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            // 重新计算点赞数和点踩数
            Comment comment = commentOptional.get();
            comment.setLikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 1));
            comment.setDislikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 2));
            Comment savedComment = commentRepository.save(comment);

            // 新增点踩时通知评论作者（不通知自己）
            if (dislikeAdded && !comment.getAuthorId().equals(userId)) {
                User targetUser = userService.findById(comment.getAuthorId());
                User fromUser = userService.findById(userId);
                Post post = postRepository.findById(comment.getPostId()).orElse(null);
                if (targetUser != null && fromUser != null && post != null) {
                    String postTitle = post.getTitle() != null ? post.getTitle() : "未知帖子";
                    notificationService.notifyForumDislike(
                            targetUser.getId(),
                            targetUser.getUsername(),
                            fromUser.getUsername(),
                            postTitle
                    );
                }
            }

            return savedComment;
        }
        return null;
    }
    
    // 获取帖子的评论数（包括所有回复）
    public int getCommentCountByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.size();
    }
}
