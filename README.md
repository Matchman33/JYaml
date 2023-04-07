# JYaml
对java实例进行yaml序列化和反序列化。侧重于序列化一个完整的类对象

待序列化Person类

    @JYamlIgnore({"name", "qq"})
    public class Person {
        
        String name;
        int age;
        String desc;
        int qq;
    
        @JYamlIgnore
        List<Phone> score = new ArrayList<>() {{
            add(new Phone());
            add(new Phone());
        }};
    
    
        @JYamlPropertyAlias("派")
        String pi;
    
        Phone phone = new Phone();
    
        HashMap<Object, HashMap<String, Phone>> map = new HashMap<>();
    
        {
            map.put("1", new HashMap<>(
                    Map.of("2", new Phone())
            ));
        }
    }

结果

    age: 20
    desc: I am a student
    派: '3.14'
    phone:
        number: 136********
        belong: SiChuan
    map:
    '1':
        '2':
            number: 136********
            belong: SiChuan

# 引擎

核心借用 snakeyaml-engine 实现

https://bitbucket.org/snakeyaml/snakeyaml-engine/src/master/
