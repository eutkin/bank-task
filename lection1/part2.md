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

Определимся с несколькими терминами:

* *Зависимость класса A типа B* -- класс А имеет поле типа B (где B должен быть обязательно интерфейсом, 
за редким исключением).

* *Обязательная зависимость* -- зависимость, без которой основная задача класс не выполнима. Например, если
некоторый сервис оперирует данными, то зависимость от репозитория будет считаться обязательной.

* *Опциональная зависимость* -- зависимость, которая не вносит какой-либо весомый вклад в выполнении задачи класса, а 
является вспомогательной, например, какой-либо фильтр, транслятор исключений и так далее. Без данной зависимости
сервис все равно способен выполнить свою бизнес-задачу. 

* *Автосвязывание* -- если у класс А есть зависимость от типа В и есть только один бин с этим типом, то Spring
автоматически внедрит зависимость, подставив бин типа В.

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
        this.repository = requiredNonNull(repository, "Repository must be not null"); // (12)
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

```java
@Configuration
public class BeansConfiguration { // (14)
    
}

public class Main {
    
    public static void main(String[] args){
      final ApplicationContext ctx = new AnnotationConfigApplicationContext(BeansConfiguration.class);
    }
}
```

Классы (2) и (3) представляют собой репозитории для манипуляции сущностьями авторов в файловой системе и базе данных 
соответственно. Сервисы (4) и (5) инкапсулируют в себе некоторую бизнес логику, которую реализализуют класс (7) и (8).

Класс (14) является java конфигурацией.

Перед примерами работы с конфигурацией, разберем класс `AuthorServiceImpl` (7).

Согласно терминам из начала лекции: 

* Зависимость `repository` (8) является обязательной зависимостью, чье наличие является крайне важной. По этому
она является финальным полем, инициализируется через конструктор и дополнительно проверяем ее на null (12). 
* Зависимость `prettryPrinter` (9) является опциональной, так как она не влияет на выполнение бизнес-задачи 
сервисом. Для ее внедрения служит `setter` (13).
* Зависимость `fieldDependency` (10) нужна, чтобы показать как пример внедрения через поле. Как мы увидим это ниже,
то через java конфиг нельзя сделать это легко и непринужденно. Несмотря на то, что это очень популярный способ 
внедрения, этот способ является в корне неверный и не рекомендуется самим Pivotal (разработчики и вендоры спринга).

Наша задача передать контроль за жизненным циклом перечисленных классов контексту спрингу. 

## Конфигурация через java-class

### Пример 1

Разберем следующий пример. Сервис `AuthorService` (4) при вызове метода `getAuthor(authorId)` должен возвращать
сущность автора. Так же необходимо выводить авторов в лог в определенном формате. Авторы должны храниться в файловой системе. Наша задача настроить конфигурация спринга таким 
образом, чтобы получить приложение, соответствующее условиям задачи. 


Идем в конфигурацию (14) и добавляем необходимы бины:

```java
@Configuration
public class BeansConfiguration { // (14)
    
    @Bean(name = "authorService") // (15)
    public AuthorService authorService(
            Repository<Author, Long> repository, // (16)
            @Autowired(requiredd = false /* 17 */ ) PrettyPrinter prettyPrinter // (18) 
        ) {
        AuthorService authorService = new AuthorServiceImpl(repository);
        if (prettyPrinter != null) {
            authorService.setPrettyPrinter(prettyPrinter); // (19)
        }
        return authorService; // (20)
    }   
    
    @Bean
    public Repository<Author, Long> fileRepository() { // (21)
        return new FileStoreRepository();
    }
    
    @Bean
    public PrettyPrinter prettyPrinter() { // (22)
        return new CapitalizePrettyPrinter();
    }
}
```

Теперь разберем пример чуть подробнее. В методе (15), который в спринге называется `factory-method`, создается
`authorService`. Для его создания нам надо получить его зависимости. Чтобы получить ссылки на другие бины, которые нужны
для создания сервиса, мы пишим их как аргументы (16) и (17) factory метода. Так как `PrettyPrinter` (5) является 
опциональной зависимостью, то мы подсказываем спрингу (17), чтобы он не выдавал ошибку, если данного бина не будет.

После этого мы можем получить через спринг контекст бин `AuthorService` и получить автора из файловой системы:

```java
public class Main {
    
    public static void main(String[] args){
      final ApplicationContext ctx = new AnnotationConfigApplicationContext(BeansConfiguration.class);
      AuthorService authorService = ctx.getBean(AuthorService.class);
      Author authorFromFile = authorService.getAuthorId(1L);
    }
}
```

В итоге мы получили искомое приложение, написав спринг конфигурацию.

### Пример 2

По мере развития нашего проекта, его аудитория разрослась, и теперь мы больше позволить себе хранить авторов в файлах.
Бизнес хочет аналитику и в этот черный день вам создали тикет мигрировать в базу данных. Так же мы решили отказаться
от вывода авторов в лог, то есть от `PrettyPrinter` (5). 

Опустим перенос существующих данных. Нам необходимо изменить способ доступа к данным и вместо файлов использовать 
базу данных. Для этих целей нам пригодится `JpaRepository` (3), который так же реализует наш интерфейс `Repository`. 
Поэтому изменим спринг конфигурацию таким образом, чтобы вместо файлового репозитория использовался 
jpa репозиторий:

```java
@Configuration
public class BeansConfiguration { // (14)
    
    @Bean(name = "authorService") // (15)
    public AuthorService authorService(
            Repository<Author, Long> repository, // (16)
            @Autowired(requiredd = false /* 17 */ ) PrettyPrinter prettyPrinter // (18) 
        ) {
        AuthorService authorService = new AuthorServiceImpl(repository);
        if (prettyPrinter != null) {
            authorService.setPrettyPrinter(prettyPrinter); // (19)
        }
        return authorService; // (20)
    }   
    
    @Bean
    public Repository<Author, Long> jpaRepository() { // (23)
        return new JpaRepository();
    }
    
}
``` 

Мы удалили бин `fileRepository` (21) и `prettyPrinter` (22) за ненадобностью и добавили новый бин
`jpaRepository` (23). Если внимательно посмотреть на конфигурацию `authorService` (15), то можно заметить, что
метод не изменился, так как мы заранее предусмотрели возможность отсутствия `prettryPrinter` и ссылаемся не
на конкретный репозиторий, а на интерфейс.

### Резюме 

Мы рассмотрели на двух примерах небольшой период жизни небольшого проекта, за который этот проект подвергся 
изменениям. Очень важный момент в этих примерах состоит в том, что не смотря на изменения требований, менялась
только конфугация спринга, а ранее написанный код (1) - (13) не менялся. 

Это происходит потому, что мы использовали такие SOLID принципы, как принцип единой ответственности и принцип
открытости-закрытости, согласно которым, каждый класс выполяет одну задачу и закрыт для модификаций, но открыт 
для расширения.  


## Конфигурация через аннотация или "Spring, сделай все за меня, братишка"


Теперь посмотрим как все будет выглядеть, если будет конфигурировать спринг через аннотации над самими классами:

```java
public interface Repository<T, ID> {} // (1)

@Repository // (2)
public class FileStoreRepository implements Repository<Author, Long> {} // (3)

public class JpaRepository implements Repository<Author, Long> {} // (4)
```
```java
public interface AuthorService {} // (5)
```
```java
public interface PrettyPrinter {}  // (6)

@Component // (7)
public class CapitalizePrettyPrinter implements PrettyPrinter {}  // (8)
```

Мы для простоты не указываем методы в интерфейсах. Посмотрим на пример использования: 

```java
@Service // (9)
public class AuthorServiceImpl implements AuthorService {  // (10)

    private final Repository<Author, Long> repository; // (11)

    private PrettyPrinter prettyPrinter; // (12)

    @Autowired // (13)
    private String fieldDependency; // (14)

    public AuthorServiceImpl(Repository<Author, Long> repository) { // (15)
        this.repository = requiredNonNull(repository, "Repository must be not null"); // (16)
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

    @Autowired(required = false) // (17)
    public void setPrettyPrinter(PrettyPrinter prettyPrinter) {  // (18)
        this.prettyPrinter = prettyPrinter;
    }
}
```

```java
@Configuration
@ComponentScan("здесь имя пакета, где лежат ваши бины") // (19)
public class BeansConfiguration { // (20)
    
}

public class Main {
    
    public static void main(String[] args){
      final ApplicationContext ctx = new AnnotationConfigApplicationContext(BeansConfiguration.class);
    }
}
```

Посмотрим, что изменилось:

* В `FileStoreRepository` (3) добавили аннотацию `@Repository` (2), которая говорит спрингу, что жизненный цикл
данного класса передают под управлению спринга. Данная аннотация является аналогом аннотации `@Component`, которую
рассмотрим чуть ниже. В отличии от нее, аннотацией `@Repository` помечаются бины, который операции по 
манипулированию сущностями в хранилищах.

* Над `CapitalizePrettyPrinter` (8) появилась аннотация `@Component` (7). Этой аннотацией помечаются все классы, 
которые мы хотим сделать бинами. Этой аннотацией следуют помечать те классы, которые не имеют отношения к хранению
данных или бизнес-логике. Например, какие-то инфраструктурные классы.

* Над сервисом `AuthorServiceImpl` (10) появилась аннотация `@Service` (9). Этой аннотацией помечаются все бины,
которые инкапсулируют в себе бизнес-логику. 

Теперь разберем как показать спрингу, какие зависимости мы хотим внедрить.

Зависимость от `Repository` (11) мы получаем через конструктор и здесь ничего дополнительно указывать не надо,
т.к. чтобы создать объект сервиса, спрингу необходимо все равно внедрить зависимость, указанную в аргументах
конструктора.

Зависимость от `PrettryPrinter` (12) мы получаем через сеттер (17), так как данная зависимость является 
опциональной. Для автосвязаывания мы используем аннотацию `@Autowired` с флагом required = false, чтобы спринг
не выдавал ошибку, в случае отсутствия бина.

Чтобы внедрить зависимость в поле `fieldDependency` (14), мы также используем данную аннотацию (13). Но еще раз 
повторю, внедрять зависимости через поле запрещено (если это, конечно, не тесты)

Последним шагом укажем в конфигурации (20) аннотацию `@ComponentScan` (19), которая подскажет спрингу, в 
каком пакете искать наши бины.

























