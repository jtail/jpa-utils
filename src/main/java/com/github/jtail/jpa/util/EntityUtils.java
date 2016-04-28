package com.github.jtail.jpa.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class EntityUtils {
    /**
     * Convenience method for easy chaining of persist calls.
     * This is very useful for use in methods that are required to return value that was just created
     *
     * @param em     entity manager
     * @param entity entity to persist
     * @param <T>    entity class
     * @return persisted entity
     */
    public static <T> T persist(EntityManager em, T entity) {
        em.persist(entity);
        return entity;
    }

    /**
     * Creates query to find entity by singular attribute
     *
     * @param em        entity manager
     * @param clazz     entity class
     * @param attribute attribute to search by
     * @param value     value to look fr
     * @return query to search for entities with a given attribute
     */
    public static <U, T> TypedQuery<U> find(EntityManager em, Class<U> clazz, SingularAttribute<U, T> attribute, T value) {
        CriteriaBuilder c = em.getCriteriaBuilder();
        CriteriaQuery<U> q = c.createQuery(clazz).distinct(true);
        Root<U> t = q.from(clazz);
        return em.createQuery(q.select(t).where(c.equal(t.get(attribute), value)));
    }

    /**
     * Finds entities by attrInB contained in collection a
     *
     * @param em
     * @param a            collection of values of attrInB to search
     * @param attrInB      attribute checked against collection a
     * @param attrToSelect resulting attribute to return
     * @param clazz        root class
     * @param <W>
     * @param <N>
     * @param <T>
     * @return
     */
    public static <W extends Entity, N extends Entity, T> Multimap<W, N> find(EntityManager em, Collection<T> a, SingularAttribute<N, T> attrInB,
                                                                              SingularAttribute<N, W> attrToSelect, Class<N> clazz) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<N> bRoot = cq.from(clazz);
        cq.multiselect(bRoot.get(attrToSelect), bRoot);
        List<Tuple> tuples = em.createQuery(cq.where(bRoot.get(attrInB).in(a))).getResultList();
        Multimap<W, N> result = ArrayListMultimap.create();
        for (Tuple tuple : tuples) {
            result.put(tuple.get(bRoot.get(attrToSelect)), tuple.get(bRoot));
        }
        return result;
    }

    /**
     * Convenience search method. Returns a request builder that can chain calls to search for multiple attributes
     *
     * @param em    Entity manager to use
     * @param clazz class to search for
     * @return request builder
     */
    public static <U> SimpleSelect<U> find(EntityManager em, Class<U> clazz) {
        return new SimpleSelect<>(em, clazz);
    }

    /**
     *
     *
     * @param <T> Search key type
     * @param <V> Target return object type
     * @param <U> key class
     * @param em entity manager
     * @param objects list of objects used as key
     * @param clazz target return object class
     * @param attribute attribute to be used in search
     * @return
     */
    public static <T, V, U extends Comparable> List<Pair<T, V>> rjoin(
            EntityManager em, List<T> objects, Class<V> clazz, SingularAttribute<V, T> attribute, Function<V, T> fnAttribute, Function<T, U> fnId
    ) {
        return rjoin(em, objects, clazz, fnAttribute, fnId, (cb, q, r) -> r.get(attribute).in(objects));
    }

    public static <T2, T extends T2, V, U extends Comparable> List<Pair<T, V>> rjoin(
            EntityManager em, List<T> objects, Class<V> clazz, Function<V, T2> fnAttribute, Function<T2, U> fnId, JPARule<V> rule
    ) {
        if (objects == null || objects.isEmpty()) {
            return Collections.emptyList();
        }

        Comparator<V> cmp = Comparator.<V, U>comparing(v -> fnId.apply(fnAttribute.apply(v)));

        List<V> results = new ArrayList<>(list(em, clazz, (cb, q) -> {
            Root<V> root = q.from(clazz);
            return q.select(root).where(rule.apply(cb, q, root));
        }));
        results.sort(cmp);

        ImmutableMap<U, V> index = Maps.uniqueIndex(results, v -> fnId.apply(fnAttribute.apply(v)));
        List<Pair<T, V>> pairs = new ArrayList<>();
        objects.forEach(o -> pairs.add(Pair.of(o, index.get(fnId.apply(o)))));
        return pairs;
    }

    /**
     * Creates query in functional form
     * @param em
     * @param clazz
     * @param fnQueryBuilder
     * @param <T>
     * @return
     */
    public static <T> TypedQuery<T> query(EntityManager em, Class<T> clazz, BiFunction<CriteriaBuilder, CriteriaQuery<T>, CriteriaQuery<T>> fnQueryBuilder) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(clazz);
        return em.createQuery(fnQueryBuilder.apply(cb, q));
    }

    /**
     * Runs list query in its functional form
     * @param em entity manager to run query
     * @param clazz target class
     * @param fnQueryBuilder Query builder function
     * @param <T> Query return type
     * @return result list
     */
    public static <T> List<T> list(EntityManager em, Class<T> clazz, BiFunction<CriteriaBuilder, CriteriaQuery<T>, CriteriaQuery<T>> fnQueryBuilder) {
        return query(em, clazz, fnQueryBuilder).getResultList();
    }

    public static <T, V> Subquery<V> subquery(CriteriaBuilder cb, AbstractQuery<? extends T> query, Class<V> clazz, JPARule<V> rule) {
        Subquery<V> sq = query.subquery(clazz);
        Root<V> root = sq.from(clazz);
        return sq.select(root).where(rule.apply(cb, sq, root));
    }

    public static <T> Predicate between(CriteriaBuilder cb, Path<T> path1, Path<T> path2, T a, T b) {
        return cb.or(
                cb.and(cb.equal(path1, a), cb.equal(path2, b)),
                cb.and(cb.equal(path1, b), cb.equal(path2, a))
        );
    }
}
