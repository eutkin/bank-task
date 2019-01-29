# Контейнер бинов и способы его конфигурации

##Вступление

Что такое бин (`bean`)? Бин это класс, жизненным циклом управляет контейнер спринга.

Основная задача контейнера бинов заключается в хранении бинов  и управлении их жизненным циклом.

Каким образом контейнер будет управлять жизненным циклом задается пользователем через _конфигурацию_ бинов, 
либо самим контейнером способом по умолчанию. 

Единственным необходимым условием включения класса в качестве 
бина под управлением контейнера - пользователь явно должен указать Spring'у, какие классы будут им 
управляться.

Делегирование управления жизненным циклом класса позволяет избавиться от большой части рутинного кода,
полноценно следовать принципу инвертирования зависимостей и сосредоточить внимание на бизнес-коде, чем на 
его инфраструктурной части.

Все бины приложения, как пользовательские, так и служебные, называются контекстом приложения и хранятся 
в `ApplicationContext`.

## Конфигурация контейнера бинов

Но, чтобы эффективно управлять жизненным циклом, спрингу необходимы подсказки от пользователя. Эти подсказки
описываются в _конфигурации бинов_ в виде декларативного описания - как создавать объект, в какие поля
необходимо внедрить зависимости, какие методы вызывать после создания и перед уничтожением объекта и т.д.

Конфигурации могут иметь зависимости друг на друга посредством импортирования. С помощью это механизма можно
иметь конфигурации всех типов, импортировав их в одну рутовую конфигурацию, через которую будет 
инициализироваться Spring контекст.

## Виды конфигурации бинов 
Есть несколько способов это сделать, давайте их перечислим:

* XML конфигурация. Описание бинов лежит в xml файле с использованием специального синтаксиса. Считается
устаревшим, используется только стариками, которые не хотят идти в ногу со временем. 
    * Плюсы: 
        * можно перегружать в рантайме
    * Минусы: 
        * можно перегружать в рантайме
        * большой объем, многословный синтаксис
        * нет типизации, но спасает IDE
* Java-class конфигурация. Описание бинов находится в java классе с использованием различных 
аннотаций.
    * Минусы: 
        * теперь вместо кучи xml пишем кучу кода
        * нельзя перегружать в рантайме 
    * Плюсы:
         * типизация, сложно ошибиться
         * код писать привычней и приятней чем xml
* Groovy скрипт конфигурация. Описание бинов лежит в groovy скрипте с использованием DSL. Появился 
со Spring 4.
    * Минусы:
        * им никто не пользуется
        * требует груви зависимостей
    * Плюсы:
        * есть типизация
        * можно перегружать в рантайме (ха-ха, прощай xml)
        * немногословный синтаксис (привет, java-конфигурация)         
* Kotlin конфигурация. Появилась в Spring 5. Бины описываются с помощью DSL, написанном на котлине. Тоже скрипт. По сути 
все тоже самое, что и груви, только типизация не динамическая, а статическая, что не может не радовать.
Must have при написании проектов Spring 5 + Kotlin.

## Примеры

Xml конфигурация:

```xml
<?xml version = "1.0" encoding = "UTF-8"?>

<beans xmlns = "http://www.springframework.org/schema/beans"
   xmlns:xsi = "http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation = "http://www.springframework.org/schema/beans
   http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">

   <bean id = "converterBean" class = "NumberToCharacterConverter" destroy-method="close">
   </bean>
   
   <bean id="someService" class="SomeServiceImpl" init-method="init" scope="prototype">
     <constructor-arg>converter</constructor-arg>
   </bean>

</beans>
```

Java-class конфигурация:     
```java
@Configuration
public class Beans {
    
    @Bean(destroyMethod = "close")
    public Converter<Number, Character> converterBean() {
        return new NumberToCharacterConverter();
    }
    
    @Bean(initMethod = "init")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public SomeService someService(Converter<Number, Character> converter) {
        return new SomeServiceImpl(converter);
    }
}
```        

Groovy конфигурация:

```groovy
beans {
    converterBean(NumberToCharacterConverter) {bean ->
        bean.destroyMethod = "close" 
    }
    
    someService(SomeServiceImpl, converter : converterBean) {bean ->
        bean.initMethod = "init" 
        bean.scope = "prototype"     
    }
    
}
```

Kotlin конфигурация:

```kotlin
fun beans() = beans {

    bean<NumberToCharacterConverter>("converterBean")
    
    bean<SomeService>("someService") {
        SomeServiceImpl(ref())
    }
}
```
(Да, я не знаю, где в котлине destroy, init методы, так как даже примеры оказалось найти не так просто)

Отметим еще способ объявления бина через аннотация. Технически это сложно назвать способом конфигурации,
так как он довольно сильно отличается от других способов. Идея этого способа состоит в том, что в 
самом классе расставить аннотации и указать на него спрингу. Спрингу считает определение бина из этих
аннотаций и класс попадает по управление контейнером.

```java
@Component
public class NumberToCharacterConverter implements Converter<Number, Character> {
 
    @Override
    public Character convert(Number source) {
    	return (char) source.shortValue();
    }
    
    @PreDestroy
    private void destroy() {
        // это приватный метод-деструктор. Да, destroy методы и init могут быть приватными
        // он нужен закрывать различные ресурсы
    }
}

@Service // это псевдоним для @Component. Он нужен чтобы подчеркнуть, что в классе находится бизнес-логика
public class SomeServiceImpl implements SomeService {
    
    private final Converter<Number, Character> converter;
    
    public SomeServiceImpl(Converter<Number, Character> converter) {
        this.converter = converter;
    }
    
    @PostConstruct
    public void init() {
        // это так называемый второй конструктор. 
        // он нужен, чтобы выполнить какие-то инициализирующие действия уже после
        // инжекта всех зависимостей
    }
    
    public void businessLogic() {
        converter.convert(1);
    }
}
```

## Резюме

Как видим из примеров, мы можем указать спрингу имя бина, какой именно класс мы передаем под его 
управление, описываем его зависимости, _init_, _destroy_ методы (выполняются после инициализации и 
до уничтожения соответственно).

Далее мы рассмотрим непосредственно правило инжекции зависимостей