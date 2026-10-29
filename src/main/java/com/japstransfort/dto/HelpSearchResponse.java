package com.japstransfort.dto;

import java.util.ArrayList;
import java.util.List;

public class HelpSearchResponse {
    private boolean found;
    private String avatarMood;
    private String query;
    private String message;
    private List<HelpResultItem> results = new ArrayList<>();
    private List<HelpQuickChip> quickChips = new ArrayList<>();

    public HelpSearchResponse() {
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getAvatarMood() {
        return avatarMood;
    }

    public void setAvatarMood(String avatarMood) {
        this.avatarMood = avatarMood;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<HelpResultItem> getResults() {
        return results;
    }

    public void setResults(List<HelpResultItem> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public List<HelpQuickChip> getQuickChips() {
        return quickChips;
    }

    public void setQuickChips(List<HelpQuickChip> quickChips) {
        this.quickChips = quickChips != null ? quickChips : new ArrayList<>();
    }

    public static class HelpResultItem {
        private String id;
        private String title;
        private String answer;
        private String link;
        private String linkLabel;

        public HelpResultItem() {
        }

        public HelpResultItem(String id, String title, String answer, String link, String linkLabel) {
            this.id = id;
            this.title = title;
            this.answer = answer;
            this.link = link;
            this.linkLabel = linkLabel;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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
    }

    public static class HelpQuickChip {
        private String id;
        private String title;
        private String query;

        public HelpQuickChip() {
        }

        public HelpQuickChip(String id, String title, String query) {
            this.id = id;
            this.title = title;
            this.query = query;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}
