# Инверсия зависимостей со Spring IoC

## Инициализация контекста  

В данной части, рассмотрим работу со Spring IoC (inversion of control), одну из реализаций принципа 
Dependency Inversion.

Как же работать с контекстом спринга? Для начала его необходимо проинициализировать. 

Способ его инициализации зависит от способа конфигурации. 

Далее мы будем рассматривать все примеры на Java конфигурации, потому что это самый распространенный пример
конфигурации (процентов так 
[95%](http://lurkmore.to/95%25_%D0%BD%D0%B0%D1%81%D0%B5%D0%BB%D0%B5%D0%BD%D0%B8%D1%8F_%E2%80%94_%D0%B8%D0%B4%D0%B8%D0%BE%D1%82%D1%8B) 
в относительно новых проектах). Остальные варианты рассмотрите самостоятельно.

```java
@Configuration
public class BeansConfiguration {
    
}

public class Main {
    
    public static void main(String[] args){
      final ApplicationContext ctx = new AnnotationConfigApplicationContext(BeansConfiguration.class);
    }
}
```

В данном примере, `BeansConfiguration` это рутовый класс java-class конфигурации контекста.

В переменной `ctx` получим инициализированный контекст приложения.

## Вводная

Для примера, опишем несколько классов и интерфейсов. Из предыдущих лекций, вы должны помнить, что любой 
класс, у которого есть поведение (то есть в его методах заключена логика), должен реализовывать интерфейс,
в котором данная логика имеет формальный вид. Приступим:

```java
public interface Repository<T, ID> {} // (1)

public class FileStoreRepository implements Repository<Author, Long> {} // (2)

public class JpaRepository implements Repository<Author, Long> {} // (3)
```
```java
public interface AuthorService {} // (4)
```
```java
public interface PrettyPrinter {}  // (5)

public class CapitalizePrettyPrinter implements PrettyPrinter {}  // (6)
```

Мы для простоты не указываем методы в интерфейсах. Посмотрим на пример использования: 

```java
public class AuthorServiceImpl implements AuthorService {  // (7)

    private final Repository<Author, Long> repository; // (8)

    private PrettyPrinter prettyPrinter; // (9)

    private String fieldDependency; // (10)

    public AuthorServiceImpl(Repository<Author, Long> repository) { // (11)
        this.repository = requireNonNull(repository, "Repository must be not null"); // (12)
    }

    @Override
    public Author getAuthor(Long authorId) {
        Author author = repository.findOne(authorId);

        if (prettyPrinter != null) {  // (13)
            String prettyName = prettyPrinter.doNameAsPretty(author.getName());
            author.setName(prettyName);
        }
        return author;
    }

    public void setPrettyPrinter(PrettyPrinter prettyPrinter) {  // (13)
        this.prettyPrinter = prettyPrinter;
    }
}
```

Классы (2) и (3) представляют собой репозитории для манипуляции сущностью автороы в файловой системе и базе данных 
соответственно. Сервисы (4) и (5) инкапсулируют в себе некоторую бизнес логику, которую реализализуют класс (7) и (8).

Наша задача передать контроль за жизненным циклом перечисленных классов контексту спрингу. 