package com.iimmersao.springmimic.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PageRequest {

    public static class Sort {
        public String field;
        public boolean ascending;


        public Sort(String sortString) {
            if (sortString != null && sortString.contains(",")) {
                String[] parts = sortString.split(",");
                this.field = parts[0];
                this.ascending = !"desc".equalsIgnoreCase(parts[1]);
            } else {
                this.field = sortString;
                this.ascending = true;
            }
        }
    }

    private int page = 0;
    private int size = 10;
    private String sortBy; // e.g., "name,asc" or "createdAt,desc"
    private Map<String, Object> filters = new HashMap<>();
    private final Set<String> likeFields = new HashSet<>();

    public PageRequest() {
    }

    public PageRequest(int page, int size, String sortBy, Map<String, Object> filters) {
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.filters = filters;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0); // prevent negative pages
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.max(size, 1); // minimum size of 1
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public void addLikeField(String fieldName) {
        likeFields.add(fieldName);
    }

    public Set<String> getLikeFields() {
        return likeFields;
    }

    public boolean isLikeField(String fieldName) {
        return likeFields.contains(fieldName);
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "page=" + page +
                ", size=" + size +
                ", sort='" + sortBy + '\'' +
                '}';
    }
}
