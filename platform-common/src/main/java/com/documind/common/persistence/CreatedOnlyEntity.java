package com.documind.common.persistence;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class CreatedOnlyEntity extends CreatedAtEntity {
}
