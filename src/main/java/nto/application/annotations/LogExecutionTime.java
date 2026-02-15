package nto.application.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // Применяется только к методам
@Retention(RetentionPolicy.RUNTIME) // Доступна в рантайме (через рефлексию)
public @interface LogExecutionTime {
}