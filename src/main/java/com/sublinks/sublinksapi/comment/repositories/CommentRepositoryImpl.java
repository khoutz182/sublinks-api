package com.sublinks.sublinksapi.comment.repositories;

import static com.sublinks.sublinksapi.utils.PaginationUtils.applyPagination;

import com.sublinks.sublinksapi.authorization.entities.Role;
import com.sublinks.sublinksapi.authorization.enums.RoleTypes;
import com.sublinks.sublinksapi.comment.entities.Comment;
import com.sublinks.sublinksapi.comment.models.CommentSearchCriteria;
import com.sublinks.sublinksapi.community.entities.Community;
import com.sublinks.sublinksapi.person.entities.LinkPersonPerson;
import com.sublinks.sublinksapi.person.entities.Person;
import com.sublinks.sublinksapi.person.enums.LinkPersonPersonType;
import com.sublinks.sublinksapi.person.enums.ListingType;
import com.sublinks.sublinksapi.shared.RemovedState;
import io.hypersistence.utils.hibernate.query.SQLExtractor;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CommentRepositoryImpl implements CommentRepositorySearch {

  private final EntityManager em;

  @Override
  public List<Comment> allCommentsBySearchCriteria(CommentSearchCriteria commentSearchCriteria) {

    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<Comment> cq = cb.createQuery(Comment.class);

    final Root<Comment> commentTable = cq.from(Comment.class);

    final List<Predicate> predicates = new ArrayList<>();

    // Parent Comment
    int maxDepth = commentSearchCriteria.maxDepth() != null && commentSearchCriteria.maxDepth() > 0
        ? commentSearchCriteria.maxDepth() : 2;

    String regex = "^0\\.(?:[0-9]+\\.?){1," + (maxDepth + 1) + "}$";

    if (commentSearchCriteria.parent() != null) {
      regex = "^" + commentSearchCriteria.parent()
          .getPath() + "\\.(?:[0-9]+\\.?){1," + (maxDepth + 1) + "}$";
    }
    predicates.add(cb.isTrue(
        cb.function("regexp_like", Boolean.class, commentTable.get("path"), cb.literal(regex))));

    // Post
    if (commentSearchCriteria.post() != null) {
      predicates.add(cb.equal(commentTable.get("post"), commentSearchCriteria.post()));
    }
    // Join for CommentView
//    if (commentSearchCriteria.person() != null) {
//      final Join<Comment, CommentRead> commentReadJoin = commentTable.join("commentReads",
//          JoinType.LEFT);
//      commentReadJoin.on(cb.equal(commentReadJoin.get("person"), commentSearchCriteria.person()));
//    }

    if (commentSearchCriteria.community() != null) {
      predicates.add(cb.equal(commentTable.get("community"), commentSearchCriteria.community()));
    }

    if (commentSearchCriteria.person() != null && (commentSearchCriteria.listingType() == null
        || !commentSearchCriteria.listingType()
        .equals(ListingType.ModeratorView))) {
      final Join<Comment, Person> personJoin = commentTable.join("person", JoinType.LEFT);
      final Join<Person, LinkPersonPerson> linkPersonPersonJoin = personJoin.join(
          "linkPersonPerson", JoinType.LEFT);
      linkPersonPersonJoin.on(
          cb.equal(linkPersonPersonJoin.get("fromPerson"), commentSearchCriteria.person()));

      final Join<LinkPersonPerson, Person> linkToPersonPersonPersonJoin = linkPersonPersonJoin.join(
          "toPerson", JoinType.LEFT);

      final Join<Person, Role> roleJoin = linkToPersonPersonPersonJoin.join("role", JoinType.LEFT);
      roleJoin.on(cb.equal(linkToPersonPersonPersonJoin.get("role"), roleJoin));

      predicates.add(cb.or(cb.isNull(linkPersonPersonJoin.get("linkType")),
          cb.notEqual(linkPersonPersonJoin.get("linkType"), LinkPersonPersonType.blocked),
          cb.notEqual(roleJoin.get("name"), RoleTypes.ADMIN.toString())));
    }

    cq.where(predicates.toArray(new Predicate[0]));

    // @todo determine hot / top / (controversial)

    switch (commentSearchCriteria.commentSortType()) {
      case New:
        cq.orderBy(cb.desc(commentTable.get("createdAt")));
        break;
      case Old:
        cq.orderBy(cb.asc(commentTable.get("createdAt")));
        break;
      default:
        cq.orderBy(cb.desc(commentTable.get("createdAt")));
        break;
    }

    final int perPage = Math.min(Math.abs(commentSearchCriteria.perPage()), 50);
    final int page = Math.max(commentSearchCriteria.page() - 1, 1);

    final TypedQuery<Comment> query = em.createQuery(cq);
    System.out.println(SQLExtractor.from(query));
    applyPagination(query, page, perPage);

    return query.getResultList();
  }

  @Override
  public List<Comment> allCommentsByCommunityAndPersonAndRemoved(Community community, Person person,
      @Nullable List<RemovedState> removedStates) {

    if (community == null || person == null) {
      throw new IllegalArgumentException("Community and person must be provided");
    }

    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<Comment> cq = cb.createQuery(Comment.class);

    final Root<Comment> commentTable = cq.from(Comment.class);

    final List<Predicate> predicates = new ArrayList<>();

    if (removedStates != null) {
      Expression<RemovedState> removedStateExpression = commentTable.get("removedState");
      predicates.add(removedStateExpression.in(removedStates));
    }

    predicates.add(cb.equal(commentTable.get("community"), community));

    predicates.add(cb.equal(commentTable.get("person"), person));

    cq.where(predicates.toArray(new Predicate[0]));

    return em.createQuery(cq)
        .getResultList();
  }

  @Override
  public List<Comment> allCommentsByPersonAndRemoved(Person person,
      @Nullable List<RemovedState> removedStates) {

    if (person == null) {
      throw new IllegalArgumentException("Person must be provided");
    }

    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<Comment> cq = cb.createQuery(Comment.class);

    final Root<Comment> commentTable = cq.from(Comment.class);

    final List<Predicate> predicates = new ArrayList<>();

    if (removedStates != null) {
      Expression<RemovedState> removedStateExpression = commentTable.get("removedState");
      predicates.add(removedStateExpression.in(removedStates));
    }

    predicates.add(cb.equal(commentTable.get("person"), person));

    cq.where(predicates.toArray(new Predicate[0]));

    return em.createQuery(cq)
        .getResultList();
  }
}
