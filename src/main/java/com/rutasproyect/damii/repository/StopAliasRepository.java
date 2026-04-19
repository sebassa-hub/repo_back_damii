package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.StopAlias;

@Repository
public interface StopAliasRepository extends JpaRepository<StopAlias, Integer> {
}
