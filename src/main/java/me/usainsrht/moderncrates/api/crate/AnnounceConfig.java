package me.usainsrht.moderncrates.api.crate;

import me.usainsrht.yamlmessage.YamlMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for crate reward announcements.
 */
public class AnnounceConfig {

    private boolean toEveryone = true;
    private String single;
    private String multiple;
    private String multipleItem;

    private YamlMessage singleMessage = YamlMessage.empty();
    private YamlMessage multipleMessage = YamlMessage.empty();
    private YamlMessage multipleItemMessage = YamlMessage.empty();

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
        this.singleMessage = YamlMessage.parse(single);
    }

    public String getMultiple() {
        return multiple;
    }

    public void setMultiple(String multiple) {
        this.multiple = multiple;
        this.multipleMessage = YamlMessage.parse(multiple);
    }

    public String getMultipleItem() {
        return multipleItem;
    }

    public void setMultipleItem(String multipleItem) {
        this.multipleItem = multipleItem;
        this.multipleItemMessage = YamlMessage.parse(multipleItem);
    }

    public @NotNull YamlMessage getSingleMessage() {
        return singleMessage != null ? singleMessage : YamlMessage.empty();
    }

    public void setSingleMessage(@Nullable YamlMessage singleMessage) {
        this.singleMessage = singleMessage != null ? singleMessage : YamlMessage.empty();
        if (singleMessage != null && singleMessage.chat() != null && !singleMessage.chat().isEmpty()) {
            this.single = String.join("\n", singleMessage.chat());
        }
    }

    public @NotNull YamlMessage getMultipleMessage() {
        return multipleMessage != null ? multipleMessage : YamlMessage.empty();
    }

    public void setMultipleMessage(@Nullable YamlMessage multipleMessage) {
        this.multipleMessage = multipleMessage != null ? multipleMessage : YamlMessage.empty();
        if (multipleMessage != null && multipleMessage.chat() != null && !multipleMessage.chat().isEmpty()) {
            this.multiple = String.join("\n", multipleMessage.chat());
        }
    }

    public @NotNull YamlMessage getMultipleItemMessage() {
        return multipleItemMessage != null ? multipleItemMessage : YamlMessage.empty();
    }

    public void setMultipleItemMessage(@Nullable YamlMessage multipleItemMessage) {
        this.multipleItemMessage = multipleItemMessage != null ? multipleItemMessage : YamlMessage.empty();
        if (multipleItemMessage != null && multipleItemMessage.chat() != null && !multipleItemMessage.chat().isEmpty()) {
            this.multipleItem = String.join("\n", multipleItemMessage.chat());
        }
    }
}

