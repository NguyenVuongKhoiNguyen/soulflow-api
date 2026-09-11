package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import com.poly.models.entities.Account;
import com.poly.models.entities.Comment;
import com.poly.models.entities.Product;
import com.poly.models.repositories.CommentRepository;
import com.poly.models.requests.CommentRequest;
import com.poly.models.responses.CommentResponse;
import com.poly.models.services.ImageService;

import jakarta.persistence.EntityNotFoundException;


@Mapper(componentModel = "spring", uses = {ReplyMapper.class})
public abstract class CommentMapper {

	@Autowired
	protected CommentRepository commentRepo;

	@Autowired
	protected ImageService imageService;

	@Mapping(target = "createdDate", 		ignore = true)
	@Mapping(target = "id", 				source = "id")
	@Mapping(target = "product",     		ignore = true)
	@Mapping(target = "account",     		ignore = true)
	@Mapping(target = "replies",     		ignore = true)
	@Mapping(target = "deleted",     		ignore = true)
	public abstract Comment toEntity(CommentRequest request); 
	
	@Mapping(target = "createdDate", 		source = "createdDate", 			dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id", 				source = "id")
	@Mapping(target =  "username", 			source = "account.username")
	@Mapping(target =  "fullname", 			source = "account.fullname")
	@Mapping(target =  "photo", 			source = "account.photo")
	@Mapping(target = "productId", 			source = "product.id")
	@Mapping(target = "accountId", 			source = "account.id")
	@Mapping(target = "url",				ignore = true)
	@Mapping(target = "replyResponses", 	ignore = true)
	@Named("basicResponse")
	public abstract CommentResponse toBasicResponse(Comment comment);

	@Mapping(target = "createdDate", 		source = "createdDate", 			dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id", 				source = "id")
	@Mapping(target =  "username", 			source = "account.username")
	@Mapping(target =  "fullname", 			source = "account.fullname")
	@Mapping(target =  "photo", 			source = "account.photo")
	@Mapping(target = "productId", 			source = "product.id")
	@Mapping(target = "accountId", 			source = "account.id")
	@Mapping(target = "replyResponses", 	source = "replies")
	@Mapping(target = "url",				ignore = true)
	@Named("detailedResponse")
	public abstract CommentResponse toDetailedResponse(Comment comment);

	@Named("basicCommentResponseList")
	@IterableMapping(qualifiedByName = "basicResponse")
	public abstract List<CommentResponse> toBasicResponseList(List<Comment> comments);
	
	@Named("detailedCommentResponseList")
	@IterableMapping(qualifiedByName = "detailedResponse")
	public abstract List<CommentResponse> toDetailedResponseList(List<Comment> comments);

	@AfterMapping
	protected void afterToEntity(CommentRequest request, @MappingTarget Comment comment) {
		Long id = comment.getId();
		if (id != null) {
			Comment oldComment = commentRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + id));
			comment.setCreatedDate(oldComment.getCreatedDate());
			comment.setProduct(oldComment.getProduct());
			comment.setAccount(oldComment.getAccount());
			comment.setReplies(oldComment.getReplies());
			comment.setDeleted(oldComment.getDeleted());
			return;
		};
		comment.setCreatedDate(LocalDateTime.now());
		Product product = new Product();
		product.setId(request.getProductId());
		Account account = new Account();
		account.setId(request.getAccountId());
		comment.setProduct(product);
		comment.setAccount(account);
		comment.setDeleted(false);
		System.out.println(comment.getDeleted());
		System.out.println(comment.getCreatedDate());
	}

	@AfterMapping
	protected void afterToResponse(@MappingTarget CommentResponse response, Comment comment) {
		String photo = comment.getAccount().getPhoto();
		if (photo != null && photo.isBlank()) {
			return;
		}
		try {
			String url = imageService.getPublicUrl(photo);
			response.setUrl(url);
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
}
