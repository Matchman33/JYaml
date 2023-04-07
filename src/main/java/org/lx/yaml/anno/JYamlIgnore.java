package org.lx.yaml.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

public @interface JYamlIgnore {
    /**
     * 修饰类或者属性，用于忽略属性或者类
     * 修饰属性时，不需要指定value
     */
    String[] value() default {};
}
