package com.github.jtail.jpa.util;

import com.github.jtail.jpa.util.rules.PredicateRule;
import com.github.jtail.jpa.util.rules.SubqueryPredicateRule;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
        return add(f.apply(cb, root));
    }

    public SimpleSelect<U> by(PredicateRule<U, U, AbstractQuery<? extends U>> f) {
        return add(f.apply(cb, root, query));
    }

    public <V> SimpleSelect<U> bySubquery(Class<V> clazz, SubqueryPredicateRule<U, V> builder) {
        return add(builder.apply(cb, root, query.subquery(clazz)));
    }

    public <V> SimpleSelect<U> hasNull(SingularAttribute<? super U, V> attribute) {
        return add(root.get(attribute).isNull());
    }

    public <V> SimpleSelect<U> has(SingularAttribute<? super U, V> attribute) {
        return add(root.get(attribute).isNotNull());
    }

    public <V> SimpleSelect<U> has(SingularAttribute<? super U, V> attribute, V value) {
        return by((cb, root) -> cb.equal(root.get(attribute), value));
    }

    public <V> SimpleSelect<U> has(ListAttribute<? super U, V> attribute, V value) {
        return add(cb.equal(root.join(attribute), value));
    }

    public <V, W> SimpleSelect<U> has(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, W value) {
        return add(cb.equal(root.get(attr1).get(attr2), value));
    }

    public <V, W> SimpleSelect<U> has(ListAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, W value) {
        return add(cb.equal(root.join(attr1).get(attr2), value));
    }

    public <V, W, X> SimpleSelect<U> has(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, SingularAttribute<? super W, X> attr3, X value) {
        return add(cb.equal(root.get(attr1).get(attr2).get(attr3), value));
    }

    public <V, W, X> SimpleSelect<U> has(ListAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, SingularAttribute<? super W, X> attr3, X value) {
        return add(cb.equal(root.join(attr1).get(attr2).get(attr3), value));
    }

    /**
     * Uses lambda expression to build complex predicates directly
     * @param fn function to build predicate
     * @return This object for easy call chaining
     */
    public SimpleSelect<U> on(BiFunction<CriteriaBuilder, Root<U>, Predicate> fn) {
        return add(fn.apply(cb, root));
    }

    public <V> SimpleSelect<U> in(SingularAttribute<? super U, V> attribute, Collection<V> value) {
        return add(value.isEmpty() ? cb.disjunction() : root.get(attribute).in(value));
    }

    public <V, W> SimpleSelect<U> in(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, W[] value) {
        return add(value.length == 0 ? cb.disjunction() : root.get(attr1).get(attr2).in(value));
    }

    public <V, W> SimpleSelect<U> in(SingularAttribute<? super U, V> attr1, SingularAttribute<? super V, W> attr2, Collection<W> value) {
        return add(value.isEmpty() ? cb.disjunction() : root.get(attr1).get(attr2).in(value));
    }

    public <V extends Comparable<? super V>> SimpleSelect<U> lt(SingularAttribute<? super U, V> attribute, V value) {
        return add(cb.lessThan(root.get(attribute), value));
    }

    public <V extends Comparable<? super V>> SimpleSelect<U> gt(SingularAttribute<? super U, V> attribute, V value) {
        return add(cb.greaterThan(root.get(attribute), value));
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

    private SimpleSelect<U> add(Predicate apply) {
        predicates.add(apply);
        return this;
    }

}
