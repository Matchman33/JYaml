package org.lx;

import org.lx.yaml.anno.JYamlIgnore;
import org.lx.yaml.anno.JYamlPropertyAlias;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;

public class JYaml {


    private final Load loader = new Load(LoadSettings.builder().build());

    private final Dump dumper = new Dump(DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).setDefaultScalarStyle(ScalarStyle.PLAIN).build());


    private final HashSet<String> BASIC_STRUCTS = new HashSet<>() {
        {
            add(String.class.getName());
            add(Integer.class.getName());
            add(Long.class.getName());
            add(Float.class.getName());
            add(Double.class.getName());
            add(Boolean.class.getName());
            add(Character.class.getName());
            add(Byte.class.getName());
            add(Short.class.getName());
            add(int.class.getName());
            add(long.class.getName());
            add(float.class.getName());
            add(double.class.getName());
            add(boolean.class.getName());
            add(char.class.getName());
            add(byte.class.getName());
            add(short.class.getName());

        }
    };

    /**
     * 获取由泛型类型构建的类型列表,通过列表长度判断是List还是Map
     *
     * @param type 泛型类型
     * @return 类型列表
     */
    private static List<Object> typeList(Type type) {
        assert type instanceof ParameterizedType;
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] types = parameterizedType.getActualTypeArguments();
        return new LinkedList<>() {
            {
                for (Type type : types) {
                    if (type instanceof ParameterizedType) {
                        add(typeList(type));
                    } else {
                        add(type);
                    }
                }
            }
        };

    }

    public String dump(Object obj) {


        try {
            return dumper.dumpToString(instanceToMap(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dump(Object obj, Writer writer) throws IOException {
        String yaml = dump(obj);

        writer.write(yaml);
        writer.flush();


    }

    public void dump(Object obj, String path) {
        try {
            dump(obj, path, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void dump(Object obj, String path, Charset charset) throws IOException {
        dump(obj, new BufferedWriter(new FileWriter(path, charset)));

    }

    /**
     * 将实例对象转换成map，实例对象应为用户自定义的类对象，不应为基本类型
     *
     * @param obj 实例对象
     * @return map
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Object instanceToMap(Object obj) throws Exception {


        if (isPrm(obj.getClass().getName())) {
            return obj;
        }
        LinkedHashMap<String, Object> resMap = new LinkedHashMap<>();

        Class<?> clazz = obj.getClass();
        HashSet<String> ignoreFieldSet = new HashSet<>() {
            {
                JYamlIgnore annotation = clazz.getAnnotation(JYamlIgnore.class);
                String[] value = annotation == null ? new String[0] : annotation.value();
                addAll(Arrays.asList(value));
            }
        };

        for (Field field : clazz.getDeclaredFields()) {
            JYamlPropertyAlias alias;
            String fieldName = (alias = field.getAnnotation(JYamlPropertyAlias.class)) != null ? alias.value() : field.getName();
            if (field.getAnnotation(JYamlIgnore.class) != null || ignoreFieldSet.contains(fieldName)) continue;
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value instanceof List) {

                List<Object> typeList = typeList(field.getGenericType());

                resMap.put(fieldName, parseList((List<Object>) value, typeList, false));

                continue;

            }
            if (value instanceof Map) {
                resMap.put(fieldName, parseMap((Map<Object, Object>) value, typeList(field.getGenericType()), false));
                continue;

            }
            resMap.put(fieldName, instanceToMap(field.get(obj)));

        }


        return resMap;
    }

    public <T> T load(Class<T> clazz, String path) {
        try {
            return load(path, Charset.defaultCharset(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T load(String path, Charset charset, Class<T> clazz) throws IOException {
        return load(new BufferedReader(new FileReader(path, charset)), clazz);
    }

    public <T> T load(BufferedReader reader, Class<T> clazz) throws IOException {

        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        reader.close();


        return load(sb.toString(), clazz);

    }

    @SuppressWarnings("unchecked")
    public <T> T load(String yaml, Class<T> clazz) {


        // 遍历yaml中的每个key，如果clazz中有对应的set方法，则调用set方法
        HashMap<String, Object> map = (HashMap<String, Object>) loader.loadFromString(yaml);
        try {
            return mapToInstance(map, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T mapToInstance(Object obj, Class<T> clazz) throws Exception {
        if (isPrm(obj.getClass().getName())) {
            return (T) obj;
        }
        Map<String, Object> map = (Map<String, Object>) obj;
        HashSet<String> ignoreFieldSet = new HashSet<>() {
            {
                JYamlIgnore annotation = clazz.getAnnotation(JYamlIgnore.class);
                String[] value = annotation == null ? new String[0] : annotation.value();
                addAll(Arrays.asList(value));
            }
        };

        T instance;
        instance = clazz.getDeclaredConstructor().newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            JYamlPropertyAlias alias;
            String fieldName = (alias = field.getAnnotation(JYamlPropertyAlias.class)) != null ? alias.value() : field.getName();
            if (field.getAnnotation(JYamlIgnore.class) != null || ignoreFieldSet.contains(fieldName)) continue;
            field.setAccessible(true);
            Class<?> type = field.getType();
            Object value = map.get(fieldName);
            Type genericType = field.getGenericType();
            if (value instanceof List) {

                field.set(instance, parseList((List<Object>) value, typeList(genericType), true));
                continue;
            }
            if (value instanceof Map) {
                field.set(instance, parseMap((Map<Object, Object>) value, typeList(genericType), true));

                continue;
            }
            field.set(instance, mapToInstance(map.get(fieldName), type));

        }


        return instance;


    }

    /**
     * 通过不同的类型，将value转换为对应的类型
     *
     * @param value         欲转换的值
     * @param type          转换的类型
     * @param mapToInstance 是否将map转换为实例,如果为false，则将实例转换为map,否则将map转换为实例
     * @return 转换后的值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseValue(Object value, Object type, boolean mapToInstance) throws Exception {
        if (type instanceof List) {
            List<Object> tpList = (List<Object>) type;
            switch (tpList.size()) {
                case 1:
                    value = parseList((List<Object>) value, tpList, mapToInstance);
                    break;
                case 2:
                    value = parseMap((Map<Object, Object>) value, tpList, mapToInstance);
                    break;
                default:
                    throw new RuntimeException("不支持的类型");
            }
        } else if (!isPrm(((Class) type).getName())) {
            if (mapToInstance) {
                value = mapToInstance(value, (Class<?>) type);
            } else {
                value = instanceToMap(value);
            }
        }
        return value;
    }

    /**
     * 解析Map类型的数据，如果Map中的key和value是基本类型，则直接返回，如果是自定义类型，则递归解析
     *
     * @param map           Map类型的数据
     * @param typeList      Map中key和value的类型
     * @param mapToInstance 是否将map转换为实例，如果为true，则将map转换为实例，否则将实例转换为map
     * @return 解析后的数据
     */
    private Map<Object, Object> parseMap(Map<Object, Object> map, List<Object> typeList, boolean mapToInstance) throws Exception {
        Object first = typeList.get(0);
        Object second = typeList.get(1);
        LinkedHashMap<Object, Object> resMap = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = parseValue(entry.getKey(), first, mapToInstance);
            Object value = parseValue(entry.getValue(), second, mapToInstance);
            resMap.put(key, value);
        }
        return resMap;


    }

    /**
     * 解析List类型的数据，如果List中的元素是基本类型，则直接返回，如果是自定义类型，则递归解析
     *
     * @param list          List类型的数据
     * @param typeList      List中元素的类型
     * @param mapToInstance 是否将map转换为实例，如果为true，则将map转换为实例，否则将实例转换为map
     * @return 解析后的List
     */
    @SuppressWarnings({"rawtypes"})
    private List<Object> parseList(List<Object> list, List typeList, boolean mapToInstance) throws Exception {
        LinkedList<Object> resList = new LinkedList<>();
        Object type = typeList.get(0);
        for (Object o : list) {
            o = parseValue(o, type, mapToInstance);
            resList.add(o);
        }
        return resList;
    }

    public boolean isPrm(String clzName) {
        return BASIC_STRUCTS.contains(clzName);
    }
}

