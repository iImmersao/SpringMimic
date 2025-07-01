package com.iimmersao.springmimic.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class RestClientTest {

    private RestClient restClient;

    @BeforeEach
    void setup() {
        restClient = new RestClient();
    }

    public static class Post {
        public int userId;
        public int id;
        public String title;
        public String body;
    }

    @Test
    void shouldPerformGetRequestAndParseJson() throws IOException, InterruptedException {
        Post post = restClient.get("https://jsonplaceholder.typicode.com/posts/1", Post.class);

        assertNotNull(post);
        assertEquals(1, post.id);
        assertNotNull(post.title);
    }

    @Test
    void shouldPerformPostRequestAndParseJson() throws IOException, InterruptedException {
        Post newPost = new Post();
        newPost.userId = 1;
        newPost.title = "foo";
        newPost.body = "bar";

        Post response = restClient.post("https://jsonplaceholder.typicode.com/posts", newPost, Post.class);

        assertNotNull(response);
        assertEquals("foo", response.title);
        assertEquals("bar", response.body);
        assertEquals(1, response.userId);
        assertTrue(response.id > 0); // new post ID assigned by server
    }
}
