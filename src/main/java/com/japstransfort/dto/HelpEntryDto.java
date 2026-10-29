package com.japstransfort.dto;

import java.util.List;

public class HelpEntryDto {
    private String id;
    private List<String> roles;
    private String title;
    private List<String> keywords;
    private String answer;
    private String link;
    private String linkLabel;
    private Boolean quickChip;

    public HelpEntryDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLinkLabel() {
        return linkLabel;
    }

    public void setLinkLabel(String linkLabel) {
        this.linkLabel = linkLabel;
    }

    public Boolean getQuickChip() {
        return quickChip;
    }

    public void setQuickChip(Boolean quickChip) {
        this.quickChip = quickChip;
    }
}
