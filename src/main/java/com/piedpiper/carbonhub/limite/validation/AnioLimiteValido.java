package com.piedpiper.carbonhub.limite.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AnioLimiteValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnioLimiteValido {
    String message() default "Seleccione un anio valido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
