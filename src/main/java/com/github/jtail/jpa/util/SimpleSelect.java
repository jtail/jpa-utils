package com.github.jtail.jpa.util;

import com.github.jtail.util.function.TriFunction;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 */
public class SimpleSelect<U> {
    protected final EntityManager em;
    protected final Class<U> clazz;
    protected final CriteriaBuilder cb;
    protected final CriteriaQuery<U> query;
    protected final Root<U> root;
    protected final List<Predicate> predicates = new LinkedList<>();

    public SimpleSelect(EntityManager em, Class<U> clazz) {
        this.em = em;
        this.clazz = clazz;
        cb = em.getCriteriaBuilder();
        query = cb.createQuery(clazz).distinct(true);
        root = query.from(clazz);
    }

    public <V> SimpleSelect<U> sort(SingularAttribute<? super U, V> attribute, boolean asc) {
        return asc ? asc(attribute) : desc(attribute);
    }

    public <V> SimpleSelect<U> asc(SingularAttribute<? super U, V> attribute) {
        query.orderBy(cb.asc(root.get(attribute)));
        return this;
    }

    public <V> SimpleSelect<U> desc(SingularAttribute<? super U, V> attribute) {
        query.orderBy(cb.desc(root.get(attribute)));
        return this;
    }

    public SimpleSelect<U> by(BiFunction<CriteriaBuilder, Root<U>, Predicate> f) {
        predicates.add(f.apply(cb, root));
        return this;
    }

    public SimpleSelect<U> by(JPARule<U> f) {
        predicates.add(f.apply(cb, query, root));
        return this;
    }

    public <V> SimpleSelect<U> bySubquery(Class<V> clazz, TriFunction<CriteriaBuilder, Root<U>, Subquery<V>, Predicate> f) {
        predicates.add(f.apply(cb, root, query.subquery(clazz)));
        return this;
    }

    public <V> SimpleSelect<U> hasNull(SingularAttribute<? super U, V> attribute) {
        predicates.add(root.get(attribute).isNull());
        return this;
    }

    public <V> SimpleSelect<U> has(SingularAttribute<? super U, V> attribute) {
        predicates.add(root.get(attribute).isNotNull());
        return this;
    }

    public <V> SimpleSelect<U> has(SingularAttribute<? super U, V> attribute, V value) {
        return by((cb, root) -> cb.equal(root.get(attribute), value));
    }

    public <V> SimpleSelect<U> has(ListAttribute<? super U, V> attribute, V value) {
        predicates.add(cb.equal(root.join(attribute), value));
        return this;
    }

    public <V, W> SimpleSelect<U> has(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, W value) {
        predicates.add(cb.equal(root.get(attr1).get(attr2), value));
        return this;
    }

    public <V, W> SimpleSelect<U> has(ListAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, W value) {
        predicates.add(cb.equal(root.join(attr1).get(attr2), value));
        return this;
    }

    public <V, W, X> SimpleSelect<U> has(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, SingularAttribute<? super W, X> attr3, X value) {
        predicates.add(cb.equal(root.get(attr1).get(attr2).get(attr3), value));
        return this;
    }

    public <V, W, X> SimpleSelect<U> has(ListAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, SingularAttribute<? super W, X> attr3, X value) {
        predicates.add(cb.equal(root.join(attr1).get(attr2).get(attr3), value));
        return this;
    }

    /**
     * Uses lambda expression to build complex predicates directly
     * @param fn function to build predicate
     * @return This object for easy call chaining
     */
    public SimpleSelect<U> on(BiFunction<CriteriaBuilder, Root<U>, Predicate> fn) {
        predicates.add(fn.apply(cb, root));
        return this;
    }

    public <V> SimpleSelect<U> in(SingularAttribute<? super U, V> attribute, Collection<V> value) {
        predicates.add(value.isEmpty() ? cb.disjunction() : root.get(attribute).in(value));
        return this;
    }

    public <V, W> SimpleSelect<U> in(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, W[] value) {
        predicates.add(value.length == 0 ? cb.disjunction() : root.get(attr1).get(attr2).in(value));
        return this;
    }

    public <V, W> SimpleSelect<U> in(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, Collection<W> value) {
        predicates.add(value.isEmpty() ? cb.disjunction() : root.get(attr1).get(attr2).in(value));
        return this;
    }

    public <V extends Comparable<? super V>> SimpleSelect<U> lt(SingularAttribute<? super U, V> attribute, V value) {
        predicates.add(cb.lessThan(root.get(attribute), value));
        return this;
    }

    public <V extends Comparable<? super V>> SimpleSelect<U> gt(SingularAttribute<? super U, V> attribute, V value) {
        predicates.add(cb.greaterThan(root.get(attribute), value));
        return this;
    }

    public U single() {
        return query().getSingleResult();
    }

    public Optional<U> optional() {
        try {
            return Optional.of(query().getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public <X extends Exception> U required(Supplier<X> supplier) throws X {
        try {
            return query().getSingleResult();
        } catch (NoResultException e) {
            throw supplier.get();
        }
    }

    public Stream<U> stream() {
        return list().stream();
    }

    /**
     * returns list of <U>elements or empty list if nothing found
     */
    public List<U> list() {
        return query().getResultList();
    }

    public List<U> list(int maxResult) {
        return query().setMaxResults(maxResult).getResultList();
    }

    /**
     *
     * @return first entity in the result set or null
     */
    public Optional<U> first() {
        return list(1).stream().findFirst();
    }


    protected TypedQuery<U> query() {
        return em.createQuery(query.select(root).where(predicates.toArray(new Predicate[predicates.size()])));
    }
}
