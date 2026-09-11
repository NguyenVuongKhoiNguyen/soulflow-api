package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Account;
import com.poly.models.entities.Comment;
import com.poly.models.entities.Reply;
import com.poly.models.repositories.ReplyRepository;
import com.poly.models.requests.ReplyRequest;
import com.poly.models.responses.ReplyResponse;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring")
public abstract class ReplyMapper {
    
    @Autowired
    protected ReplyRepository replyRepo;

    @Mapping(target = "createdDate",    ignore = true)
    @Mapping(target = "id",             source = "id")
    @Mapping(target = "comment",        ignore = true)
    @Mapping(target = "account",        ignore = true)
    @Mapping(target = "deleted",        ignore = true)
    public abstract Reply toEntity(ReplyRequest request); 

    @Mapping(target = "createdDate", 	source = "createdDate", 			dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "id",             source = "id")
    @Mapping(target = "username",       source = "account.username")
    @Mapping(target = "fullname",       source = "account.fullname")
    @Mapping(target = "photo",          source = "account.photo")
    @Mapping(target = "accountId",      source = "account.id")
    @Mapping(target = "commentId",      source = "comment.id")
    public abstract ReplyResponse toResponse(Reply reply);

    public abstract List<Reply> toEntityList(List<ReplyRequest> replyRequests);

    public abstract List<ReplyResponse> toResponseList(List<Reply> replies);

    @AfterMapping
    protected void afterToEntity(ReplyRequest request, @MappingTarget Reply reply) {
        Long id = reply.getId();
        if (id != null) {
            Reply oldReply = replyRepo.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Reply not found with id: " + id)
            );
            reply.setCreatedDate(oldReply.getCreatedDate());
            reply.setComment(oldReply.getComment());
            reply.setAccount(oldReply.getAccount());
            reply.setDeleted(oldReply.getDeleted());
            return;
        }
        reply.setCreatedDate(LocalDateTime.now());
        Comment comment = new Comment();
        Account account = new Account();
        comment.setId(request.getCommentId());
        account.setId(request.getAccountId());
        reply.setComment(comment);
        reply.setAccount(account);
        reply.setDeleted(false);
    }
}
