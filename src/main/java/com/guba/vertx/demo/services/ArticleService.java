package com.guba.vertx.demo.services;

import com.guba.vertx.demo.models.Article;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArticleService extends AbstractVerticle {

    private final List<Article> articles = new ArrayList<>();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        super.start(startPromise);
        vertx.eventBus().consumer("article.service.create", msg -> {
            JsonObject body = (JsonObject) msg.body();
            Article articleRequest = body.mapTo(Article.class);

            Article articleResponse = this.create(articleRequest);

            //return result/response
            msg.reply(JsonObject.mapFrom(articleResponse));
        });

        vertx.eventBus().consumer("article.service.getAll", msg -> {

            JsonArray jsonArray = new JsonArray();
            this.getAll().forEach(p -> jsonArray.add(JsonObject.mapFrom(p)));

            //return result/response
            msg.reply(jsonArray);
        });

        vertx.eventBus().consumer("article.service.getById", msg -> {
            String id = (String) msg.body();

            Optional<Article> articleResponse = this.getById(id);

            if (articleResponse.isPresent()) {
                msg.reply(JsonObject.mapFrom(articleResponse.get()));

            } else {
                msg.fail(404, "Id Article " + id + " Not Found");
            }
        });


    }

    public Article create(Article article) {
        articles.add(article);
        return article;
    }

    public List<Article> getAll() {
        return articles;
    }

    public Optional<Article> getById(String id) {
        return articles
                .stream()
                .filter(a-> a.getId().equals(id))
                .findFirst();
    }

}
