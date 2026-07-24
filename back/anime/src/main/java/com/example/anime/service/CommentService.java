package com.example.anime.service;

import com.example.anime.model.Comment;
import com.example.anime.model.ForumCommentInteraction;
import com.example.anime.repository.CommentRepository;
import com.example.anime.repository.ForumCommentInteractionRepository;
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

    // 添加评论
    public Comment addComment(Long postId, Long authorId, String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment.setCreateTime(new Date());
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        return commentRepository.save(comment);
    }

    // 根据帖子ID获取评论列表
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }



    // 根据用户ID获取评论列表
    public List<Comment> getCommentsByAuthorId(Long authorId) {
        return commentRepository.findByAuthorId(authorId);
    }

    // 删除评论
    public Comment deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            // 先删除评论的互动记录
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
            }
        } else {
            // 没有互动过，添加点赞
            ForumCommentInteraction interaction = new ForumCommentInteraction();
            interaction.setCommentId(commentId);
            interaction.setUserId(userId);
            interaction.setInteractionType(1); // 1: 点赞
            interaction.setCreateTime(new Date());
            forumCommentInteractionRepository.save(interaction);
        }
        
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            // 重新计算点赞数和点踩数
            Comment comment = commentOptional.get();
            comment.setLikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 1));
            comment.setDislikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 2));
            return commentRepository.save(comment);
        }
        return null;
    }

    // 点踩评论
    public Comment dislikeComment(Long commentId, Long userId) {
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
            }
        } else {
            // 没有互动过，添加点踩
            ForumCommentInteraction interaction = new ForumCommentInteraction();
            interaction.setCommentId(commentId);
            interaction.setUserId(userId);
            interaction.setInteractionType(2); // 2: 点踩
            interaction.setCreateTime(new Date());
            forumCommentInteractionRepository.save(interaction);
        }
        
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            // 重新计算点赞数和点踩数
            Comment comment = commentOptional.get();
            comment.setLikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 1));
            comment.setDislikeCount(forumCommentInteractionRepository.countByCommentIdAndInteractionType(commentId, 2));
            return commentRepository.save(comment);
        }
        return null;
    }
    
    // 获取帖子的评论数（包括所有回复）
    public int getCommentCountByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.size();
    }
}
