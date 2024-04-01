package com.wzy.selector;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class PersonImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 可以灵活的判断要注入哪些bean
        return new String[]{"com.wzy.component.Student", "com.wzy.component.Teacher"};
    }
}
