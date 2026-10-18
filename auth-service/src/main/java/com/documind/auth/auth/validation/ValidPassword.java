package com.documind.auth.auth.validation;

import com.documind.backend.common.validation.ValidationMessages;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

	String message() default ValidationMessages.PASSWORD_COMPLEXITY;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
