package com.iimmersao.springmimic.repository;

import com.iimmersao.springmimic.model.Film;

import java.util.List;

public interface FilmRepository extends CrudRepository<Film, Integer> {
    Film findByTitle(String title);
    List<Film> findByTitleContains(String title);
}
