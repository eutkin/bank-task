package net.eukin.spring.lection1.repository;

import net.eukin.spring.lection1.entity.Author;

public class JpaRepository implements Repository<Author, Long> {

    @Override
    public Author findOne(Long aLong) {
        return new Author();
    }
}
