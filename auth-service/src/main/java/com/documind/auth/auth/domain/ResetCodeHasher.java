package com.documind.auth.auth.domain;

public interface ResetCodeHasher {

	String hash(String plainCode);
}
