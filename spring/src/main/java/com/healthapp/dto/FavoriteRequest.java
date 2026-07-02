package com.healthapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FavoriteRequest {
    private String articleTitle;
    private String articleUrl;
    private String source;
    private String summary;

    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }
    @JsonProperty("article_title")
    public void setArticleTitleSnake(String articleTitle) { this.articleTitle = articleTitle; }
    public String getArticleUrl() { return articleUrl; }
    public void setArticleUrl(String articleUrl) { this.articleUrl = articleUrl; }
    @JsonProperty("article_url")
    public void setArticleUrlSnake(String articleUrl) { this.articleUrl = articleUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
