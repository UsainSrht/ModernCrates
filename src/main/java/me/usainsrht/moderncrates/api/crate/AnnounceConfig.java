package me.usainsrht.moderncrates.api.crate;

/**
 * Configuration for crate reward announcements.
 */
public class AnnounceConfig {

    private boolean toEveryone;
    private String single;
    private String multiple;
    private String multipleItem;

    public boolean isToEveryone() {
        return toEveryone;
    }

    public void setToEveryone(boolean toEveryone) {
        this.toEveryone = toEveryone;
    }

    public String getSingle() {
        return single;
    }

    public void setSingle(String single) {
        this.single = single;
    }

    public String getMultiple() {
        return multiple;
    }

    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }

    public String getMultipleItem() {
        return multipleItem;
    }

    public void setMultipleItem(String multipleItem) {
        this.multipleItem = multipleItem;
    }
}
