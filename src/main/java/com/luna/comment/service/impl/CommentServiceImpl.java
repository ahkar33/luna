package com.luna.comment.service.impl;

import com.luna.comment.dto.CommentResponse;
import com.luna.comment.dto.CreateCommentRequest;
import com.luna.comment.entity.Comment;
import com.luna.comment.repository.CommentRepository;
import com.luna.comment.service.ICommentService;
import com.luna.common.exception.BadRequestException;
import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.exception.UnauthorizedException;
import com.luna.post.entity.Post;
import com.luna.post.repository.PostRepository;
import com.luna.user.entity.User;
import com.luna.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (post.isDeleted()) {
            throw new BadRequestException("Cannot comment on a deleted post");
        }
        
        User author = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        int depth = 0;
        Comment parent = null;
        
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            
            if (!parent.getPost().getId().equals(postId)) {
                throw new BadRequestException("Parent comment does not belong to this post");
            }
            
            if (parent.getDepth() >= Comment.MAX_DEPTH) {
                throw new BadRequestException("Maximum reply depth reached. Reply to the parent comment instead.");
            }
            
            depth = parent.getDepth() + 1;
        }
        
        Comment comment = Comment.builder()
            .content(request.getContent())
            .post(post)
            .author(author)
            .parent(parent)
            .depth(depth)
            .build();
        
        comment = commentRepository.save(comment);
        
        return mapToResponse(comment, true);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own comments");
        }
        
        comment.setContent(content);
        comment = commentRepository.save(comment);
        
        return mapToResponse(comment, false);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }
        
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getPostComments(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found");
        }
        
        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId, pageable);
        
        return comments.map(comment -> mapToResponse(comment, true));
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        return mapToResponse(comment, true);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    private CommentResponse mapToResponse(Comment comment, boolean includeReplies) {
        List<CommentResponse> replies = null;
        
        if (includeReplies && comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replies = comment.getReplies().stream()
                .map(reply -> mapToResponse(reply, true))
                .collect(Collectors.toList());
        }
        
        return CommentResponse.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .author(CommentResponse.AuthorInfo.builder()
                .id(comment.getAuthor().getId())
                .username(comment.getAuthor().getUsernameField())
                .profileImageUrl(comment.getAuthor().getProfileImageUrl())
                .build())
            .postId(comment.getPost().getId())
            .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
            .depth(comment.getDepth())
            .replies(replies)
            .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
