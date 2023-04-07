package org.lx.yaml.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.FIELD})
public @interface JYamlPropertyAlias {
    /**
     * 修饰属性，用于指定属性的别名
     *
     * @return 属性的别名
     */
    String value();
}
