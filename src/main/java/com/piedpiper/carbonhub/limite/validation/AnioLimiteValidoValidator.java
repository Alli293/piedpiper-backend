package com.piedpiper.carbonhub.limite.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

public class AnioLimiteValidoValidator implements ConstraintValidator<AnioLimiteValido, Integer> {
    private static final int ANIO_MINIMO_REPORTE = 2000;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        int maxYear = Year.now().getValue() + 1;
        return value >= ANIO_MINIMO_REPORTE && value <= maxYear;
    }
}
