package com.sirket.auth.auth.domain;

public interface ResetCodeHasher {

	String hash(String plainCode);
}
