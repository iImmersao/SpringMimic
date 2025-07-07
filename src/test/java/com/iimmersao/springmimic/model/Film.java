package com.iimmersao.springmimic.model;

import com.iimmersao.springmimic.annotations.*;

@Entity
@Table(name="film")
public class Film {

    public Film() {

    }

    public Film(Integer id, String title) {
        this.id = id;
        this.title = title;
    }

    @Id
    @GeneratedValue
    @Column(name = "film_id")
    private Integer id;

    @Column(name = "title")
    private String title;


    public Integer getId() {
        return id;
    }

    public void setFilmId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}