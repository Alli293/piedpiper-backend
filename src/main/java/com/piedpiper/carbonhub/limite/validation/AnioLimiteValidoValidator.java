package com.piedpiper.carbonhub.limite.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

public class AnioLimiteValidoValidator implements ConstraintValidator<AnioLimiteValido, Integer> {
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        int maxYear = Year.now().getValue() + 1;
        return value >= 2000 && value <= maxYear;
    }
}
