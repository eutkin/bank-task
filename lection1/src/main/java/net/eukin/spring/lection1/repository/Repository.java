package net.eukin.spring.lection1.repository;

public interface Repository<T, ID> {

    T findOne(ID id);
}
