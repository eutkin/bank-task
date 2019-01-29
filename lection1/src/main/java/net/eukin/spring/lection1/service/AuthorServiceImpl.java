package net.eukin.spring.lection1.service;

import net.eukin.spring.lection1.entity.Author;
import net.eukin.spring.lection1.repository.Repository;

import static java.util.Objects.requireNonNull;

public class AuthorServiceImpl implements AuthorService {

    private final Repository<Author, Long> repository;

    private PrettyPrinter prettyPrinter;

    private String fieldDependency;

    public AuthorServiceImpl(Repository<Author, Long> repository) {
        this.repository = requireNonNull(repository, "Repository must be not null");
    }

    @Override
    public Author getAuthor(Long authorId) {
        Author author = repository.findOne(authorId);

        if (prettyPrinter != null) {
            String prettyName = prettyPrinter.doNameAsPretty(author.getName());
            author.setName(prettyName);
        }
        return author;
    }

    public void setPrettyPrinter(PrettyPrinter prettyPrinter) {
        this.prettyPrinter = prettyPrinter;
    }
}
