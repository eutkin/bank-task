package net.eukin.spring.lection1.entity;

public class Author {

    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public Author setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Author setName(String name) {
        this.name = name;
        return this;
    }
}
