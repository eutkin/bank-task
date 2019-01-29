package net.eukin.spring.lection1.repository;

import net.eukin.spring.lection1.entity.Author;

public class FileStoreRepository implements Repository<Author, Long> {

    @Override
    public Author findOne(Long aLong) {
        return new Author();
    }
}
