package com.poly.seeding;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Account;
import com.poly.models.entities.Comment;
import com.poly.models.entities.Product;
import com.poly.models.entities.Reply;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.CommentRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.repositories.ReplyRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(3)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.comment-reply-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CommentReplyDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommentReplyDataSeeder.class);
    private static final int COMMENTS_PER_PRODUCT = 10;
    private static final int REPLIES_PER_COMMENT = 3;
    private static final String COMMENT_PREFIX = "Seed comment ";
    private static final String REPLY_PREFIX = "Seed reply ";

    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> products = productRepository.findAll(Sort.by("id").ascending());
        List<Account> accounts = accountRepository
            .findByUsernameStartingWithOrderByUsernameAsc("user");

        if (products.isEmpty()) {
            log.info("Comment/reply seed skipped because no products exist");
            return;
        }
        if (accounts.isEmpty()) {
            throw new IllegalStateException(
                "Comment/reply seeding requires the user accounts created by V2"
            );
        }

        int createdComments = 0;
        int createdReplies = 0;

        for (int productIndex = 0; productIndex < products.size(); productIndex++) {
            Product product = products.get(productIndex);
            CommentSeedResult commentResult = ensureComments(product, productIndex, accounts);
            createdComments += commentResult.createdCount();
            createdReplies += ensureReplies(commentResult.comments(), productIndex, accounts);
        }

        log.info(
            "Comment/reply seed complete: {} products, {} comments created, {} replies created",
            products.size(), createdComments, createdReplies
        );
    }

    private CommentSeedResult ensureComments(Product product, int productIndex, List<Account> accounts) {
        List<Comment> existing = commentRepository
            .findByProductIdAndContentStartingWith(product.getId(), COMMENT_PREFIX);
        Map<String, Comment> commentsByPrefix = new HashMap<>();
        for (Comment comment : existing) {
            commentsByPrefix.put(commentKey(comment.getContent()), comment);
        }

        List<Comment> created = new ArrayList<>();
        for (int number = 1; number <= COMMENTS_PER_PRODUCT; number++) {
            String key = commentKey(number);
            if (commentsByPrefix.containsKey(key)) {
                continue;
            }

            Comment comment = new Comment();
            comment.setContent(key + "for " + product.getName());
            comment.setCreatedDate(LocalDateTime.now().minusDays(COMMENTS_PER_PRODUCT - number));
            comment.setDeleted(false);
            comment.setProduct(product);
            comment.setAccount(accounts.get((productIndex * COMMENTS_PER_PRODUCT + number - 1) % accounts.size()));
            created.add(comment);
        }

        if (!created.isEmpty()) {
            commentRepository.saveAllAndFlush(created);
            for (Comment comment : created) {
                commentsByPrefix.put(commentKey(comment.getContent()), comment);
            }
        }

        List<Comment> seededComments = new ArrayList<>();
        for (int number = 1; number <= COMMENTS_PER_PRODUCT; number++) {
            seededComments.add(commentsByPrefix.get(commentKey(number)));
        }
        return new CommentSeedResult(seededComments, created.size());
    }

    private int ensureReplies(List<Comment> comments, int productIndex, List<Account> accounts) {
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        List<Reply> existing = replyRepository
            .findByCommentIdInAndContentStartingWith(commentIds, REPLY_PREFIX);
        Set<String> existingKeys = new HashSet<>();
        for (Reply reply : existing) {
            existingKeys.add(reply.getComment().getId() + ":" + replyKey(reply.getContent()));
        }

        List<Reply> created = new ArrayList<>();
        for (int commentIndex = 0; commentIndex < comments.size(); commentIndex++) {
            Comment comment = comments.get(commentIndex);
            for (int number = 1; number <= REPLIES_PER_COMMENT; number++) {
                String key = replyKey(number);
                if (existingKeys.contains(comment.getId() + ":" + key)) {
                    continue;
                }

                Reply reply = new Reply();
                reply.setContent(key + "to " + comment.getContent().toLowerCase());
                reply.setCreatedDate(comment.getCreatedDate().plusMinutes(number));
                reply.setDeleted(false);
                reply.setComment(comment);
                reply.setAccount(accounts.get(
                    (productIndex * COMMENTS_PER_PRODUCT + commentIndex + number) % accounts.size()
                ));
                created.add(reply);
            }
        }

        if (!created.isEmpty()) {
            replyRepository.saveAll(created);
        }
        return created.size();
    }

    private String commentKey(int number) {
        return COMMENT_PREFIX + String.format("%02d ", number);
    }

    private String commentKey(String content) {
        int keyLength = COMMENT_PREFIX.length() + 3;
        return content.length() >= keyLength ? content.substring(0, keyLength) : content;
    }

    private String replyKey(int number) {
        return REPLY_PREFIX + String.format("%02d ", number);
    }

    private String replyKey(String content) {
        int keyLength = REPLY_PREFIX.length() + 3;
        return content.length() >= keyLength ? content.substring(0, keyLength) : content;
    }

    private record CommentSeedResult(List<Comment> comments, int createdCount) {
    }
}
