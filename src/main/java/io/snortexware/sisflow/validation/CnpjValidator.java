package io.snortexware.sisflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // let @NotBlank handle nulls

        String cnpj = value.replaceAll("[.\\-/]", "");

        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) return false;

        try {
            int sum = 0;
            int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 12; i++) sum += Character.getNumericValue(cnpj.charAt(i)) * weights1[i];
            int d1 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
            if (d1 != Character.getNumericValue(cnpj.charAt(12))) return false;

            sum = 0;
            int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            for (int i = 0; i < 13; i++) sum += Character.getNumericValue(cnpj.charAt(i)) * weights2[i];
            int d2 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
            return d2 == Character.getNumericValue(cnpj.charAt(13));
        } catch (Exception e) {
            return false;
        }
    }
}
