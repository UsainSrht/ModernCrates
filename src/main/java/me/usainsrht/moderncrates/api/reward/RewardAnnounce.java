package me.usainsrht.moderncrates.api.reward;

import me.usainsrht.yamlmessage.YamlMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for a single reward's announcement settings.
 */
public class RewardAnnounce {

    private Boolean enabled;
    private Boolean toEveryone;
    private String messageRaw;
    private YamlMessage message;

    public RewardAnnounce() {
    }

    public RewardAnnounce(@Nullable Boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable Boolean getEnabled() {
        return enabled;
    }

    public @Nullable Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(@Nullable Boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable Boolean getToEveryone() {
        return toEveryone;
    }

    public @Nullable Boolean isToEveryone() {
        return toEveryone;
    }

    public void setToEveryone(@Nullable Boolean toEveryone) {
        this.toEveryone = toEveryone;
    }

    public @Nullable YamlMessage getMessage() {
        if (message != null) return message;
        if (messageRaw != null) {
            this.message = YamlMessage.parse(messageRaw);
            return this.message;
        }
        return null;
    }

    public void setMessage(@Nullable YamlMessage message) {
        this.message = message;
        if (message != null && message.chat() != null && !message.chat().isEmpty()) {
            this.messageRaw = String.join("\n", message.chat());
        } else if (message == null) {
            this.messageRaw = null;
        }
    }

    public void setMessage(@Nullable String message) {
        this.messageRaw = message;
        this.message = message != null ? YamlMessage.parse(message) : null;
    }

    public @Nullable String getMessageRaw() {
        return messageRaw;
    }

    public void setMessageRaw(@Nullable String messageRaw) {
        setMessage(messageRaw);
    }

    public boolean hasMessage() {
        YamlMessage msg = getMessage();
        return msg != null && !msg.isEmpty();
    }

    public boolean isEmpty() {
        return enabled == null && toEveryone == null && (messageRaw == null || messageRaw.isEmpty()) && (message == null || message.isEmpty());
    }
}
