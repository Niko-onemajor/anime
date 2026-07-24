package com.example.anime.service;

import com.example.anime.model.Post;
import com.example.anime.model.User;
import com.example.anime.model.Comment;
import com.example.anime.model.ForumPostInteraction;
import com.example.anime.repository.PostRepository;
import com.example.anime.repository.UserRepository;
import com.example.anime.repository.ForumPostInteractionRepository;
// import com.example.anime.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentService commentService;
    @Autowired
    private ForumPostInteractionRepository forumPostInteractionRepository;

    // 获取所有帖子
    public List<Post> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreateTimeDesc();
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 根据ID获取帖子
    public Post getPostById(Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        }
        return post;
    }

    // 根据作者ID获取帖子
    public List<Post> getPostsByAuthorId(Long authorId) {
        List<Post> posts = postRepository.findByAuthorId(authorId);
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 按时间排序获取帖子
    public List<Post> getPostsByTime() {
        List<Post> posts = postRepository.findAllByOrderByCreateTimeDesc();
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 按点赞数排序获取帖子
    public List<Post> getPostsByLikes() {
        List<Post> posts = postRepository.findAllByOrderByLikeCountDesc();
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 按点踩数排序获取帖子
    public List<Post> getPostsByDislikes() {
        List<Post> posts = postRepository.findAllByOrderByDislikeCountDesc();
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 保存帖子
    public Post savePost(Post post, String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username);
        if (user != null) {
            post.setAuthor(user);
            post.setCreateTime(new Date());
            post.setLikeCount(0);
            post.setDislikeCount(0);
            post.setCommentCount(0);
            return postRepository.save(post);
        }
        return null;
    }

    // 更新帖子
    public Post updatePost(Post post) {
        return postRepository.save(post);
    }

    // 删除帖子
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    // 点赞帖子
    public Post likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            // 检查用户是否已经对该帖子进行过互动
            java.util.Optional<ForumPostInteraction> existingInteraction = forumPostInteractionRepository.findByUserIdAndPostId(userId, postId);
            if (existingInteraction.isPresent()) {
                // 如果已经是点赞，则取消点赞
                if (existingInteraction.get().getInteractionType() == 1) {
                    forumPostInteractionRepository.delete(existingInteraction.get());
                    post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                } else {
                    // 如果是点踩，则先删除点踩，再添加点赞
                    forumPostInteractionRepository.delete(existingInteraction.get());
                    ForumPostInteraction interaction = new ForumPostInteraction();
                    interaction.setPostId(postId);
                    interaction.setUserId(userId);
                    interaction.setInteractionType(1); // 1: 点赞
                    interaction.setCreateTime(new Date());
                    forumPostInteractionRepository.save(interaction);
                    post.setLikeCount(post.getLikeCount() + 1);
                    post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
                }
            } else {
                // 没有互动过，添加点赞
                ForumPostInteraction interaction = new ForumPostInteraction();
                interaction.setPostId(postId);
                interaction.setUserId(userId);
                interaction.setInteractionType(1); // 1: 点赞
                interaction.setCreateTime(new Date());
                forumPostInteractionRepository.save(interaction);
                post.setLikeCount(post.getLikeCount() + 1);
            }
            return postRepository.save(post);
        }
        return null;
    }

    // 点踩帖子
    public Post dislikePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            // 检查用户是否已经对该帖子进行过互动
            java.util.Optional<ForumPostInteraction> existingInteraction = forumPostInteractionRepository.findByUserIdAndPostId(userId, postId);
            if (existingInteraction.isPresent()) {
                // 如果已经是点踩，则取消点踩
                if (existingInteraction.get().getInteractionType() == 2) {
                    forumPostInteractionRepository.delete(existingInteraction.get());
                    post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
                } else {
                    // 如果是点赞，则先删除点赞，再添加点踩
                    forumPostInteractionRepository.delete(existingInteraction.get());
                    ForumPostInteraction interaction = new ForumPostInteraction();
                    interaction.setPostId(postId);
                    interaction.setUserId(userId);
                    interaction.setInteractionType(2); // 2: 点踩
                    interaction.setCreateTime(new Date());
                    forumPostInteractionRepository.save(interaction);
                    post.setDislikeCount(post.getDislikeCount() + 1);
                    post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                }
            } else {
                // 没有互动过，添加点踩
                ForumPostInteraction interaction = new ForumPostInteraction();
                interaction.setPostId(postId);
                interaction.setUserId(userId);
                interaction.setInteractionType(2); // 2: 点踩
                interaction.setCreateTime(new Date());
                forumPostInteractionRepository.save(interaction);
                post.setDislikeCount(post.getDislikeCount() + 1);
            }
            return postRepository.save(post);
        }
        return null;
    }

    // 获取帖子的点赞和点踩数
    public void updatePostInteractionCounts(Post post) {
        if (post != null) {
            int likeCount = forumPostInteractionRepository.countByPostIdAndInteractionType(post.getId(), 1);
            int dislikeCount = forumPostInteractionRepository.countByPostIdAndInteractionType(post.getId(), 2);
            post.setLikeCount(likeCount);
            post.setDislikeCount(dislikeCount);
        }
    }

    // 获取用户对帖子的互动状态
    public Integer getUserPostInteraction(Long postId, Long userId) {
        java.util.Optional<ForumPostInteraction> interaction = forumPostInteractionRepository.findByUserIdAndPostId(userId, postId);
        return interaction.map(ForumPostInteraction::getInteractionType).orElse(null);
    }

    // 增加评论数
    public void incrementCommentCount(Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            if (post.getCommentCount() == null) {
                post.setCommentCount(0);
            }
            post.setCommentCount(post.getCommentCount() + 1);
            postRepository.save(post);
        }
    }

    // 减少评论数
    public void decrementCommentCount(Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            if (post.getCommentCount() == null) {
                post.setCommentCount(0);
            } else if (post.getCommentCount() > 0) {
                post.setCommentCount(post.getCommentCount() - 1);
                postRepository.save(post);
            }
        }
    }

    // 获取所有帖子（用于管理员）
    public List<Post> findAll() {
        List<Post> posts = postRepository.findAll();
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 根据标题搜索帖子（用于管理员）
    public List<Post> findByTitleContaining(String keyword) {
        List<Post> posts = postRepository.findByTitleContaining(keyword);
        // 更新每个帖子的实际评论数和互动计数
        posts.forEach(post -> {
            int commentCount = commentService.getCommentCountByPostId(post.getId());
            post.setCommentCount(commentCount);
            updatePostInteractionCounts(post);
        });
        return posts;
    }

    // 根据ID删除帖子（用于管理员）
    public void deleteById(Long id) {
        // 先删除帖子的所有评论
        List<Comment> comments = commentService.getCommentsByPostId(id);
        for (Comment comment : comments) {
            commentService.deleteById(comment.getId());
        }
        // 再删除帖子
        postRepository.deleteById(id);
    }
}