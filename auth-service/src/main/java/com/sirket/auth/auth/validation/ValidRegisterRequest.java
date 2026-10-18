package com.sirket.auth.auth.validation;

import com.documind.backend.common.validation.ValidationMessages;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RegisterRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegisterRequest {

	String message() default ValidationMessages.PASSWORD_CONTAINS_EMAIL;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
