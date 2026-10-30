package com.japstransfort.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response payload for POST /help/ask.
 * source is either "ai" or "keyword".
 * mood is "happy" or "surprised".
 */
public class HelpAskResponse {

    private String answer;
    private String mood;
    private List<HelpLink> links = new ArrayList<>();
    private String source;

    public HelpAskResponse() {
    }

    public HelpAskResponse(String answer, String mood, List<HelpLink> links, String source) {
        this.answer = answer;
        this.mood = mood;
        this.links = links != null ? links : new ArrayList<>();
        this.source = source;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public List<HelpLink> getLinks() {
        return links;
    }

    public void setLinks(List<HelpLink> links) {
        this.links = links != null ? links : new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static class HelpLink {
        private String label;
        private String url;

        public HelpLink() {
        }

        public HelpLink(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
