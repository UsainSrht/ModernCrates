package me.usainsrht.moderncrates.api.animation;

import java.util.List;
import java.util.Map;

/**
 * Represents a configured animation - a specific customization of an AnimationType.
 * Each file in the animations/ folder produces one Animation instance.
 *
 * For example, the "csgo" animation type can be configured as a horizontal scroll
 * or modified into a vertical roulette system.
 */
public class Animation {

    private final String id;
    private String typeId;

    // Common CSGO-type fields
    private int totalTicks;
    private int stayOpenAfterRewardTicks;
    private int startTickRate;
    private int tickRateModifier;

    // Common Click-type fields
    private int shuffleAmount;
    private int shuffleTicks;
    private List<String> shuffleSounds;
    private int rewardAmount;
    private GuiItemConfig rewardHideItem;
    private List<String> hideSounds;
    private List<String> revealSounds;
    private int showRevealedItemsFor;

    // GUI settings
    private String guiTitle;
    private String guiTitleShuffling;
    private int guiRows;
    private String guiType;
    private GuiItemConfig guiFill;

    // Reward slots
    private int rewardIndex;
    private List<Integer> rewardSlots;

    // Filler
    private List<Integer> fillerSlots;
    private Map<String, GuiItemConfig> fillerItems;

    // End-of-animation override
    private GuiItemConfig endOfAnimationItem;
    private List<Integer> endOfAnimationSlots;

    // Pointers
    private PointerConfig downPointer;
    private PointerConfig upPointer;

    // Sounds
    private List<String> tickSounds;
    private List<String> rewardSounds;

    public Animation(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }

    public int getTotalTicks() { return totalTicks; }
    public void setTotalTicks(int totalTicks) { this.totalTicks = totalTicks; }

    public int getStayOpenAfterRewardTicks() { return stayOpenAfterRewardTicks; }
    public void setStayOpenAfterRewardTicks(int stayOpenAfterRewardTicks) { this.stayOpenAfterRewardTicks = stayOpenAfterRewardTicks; }

    public int getStartTickRate() { return startTickRate; }
    public void setStartTickRate(int startTickRate) { this.startTickRate = startTickRate; }

    public int getTickRateModifier() { return tickRateModifier; }
    public void setTickRateModifier(int tickRateModifier) { this.tickRateModifier = tickRateModifier; }

    public int getShuffleAmount() { return shuffleAmount; }
    public void setShuffleAmount(int shuffleAmount) { this.shuffleAmount = shuffleAmount; }

    public int getShuffleTicks() { return shuffleTicks; }
    public void setShuffleTicks(int shuffleTicks) { this.shuffleTicks = shuffleTicks; }

    public List<String> getShuffleSounds() { return shuffleSounds; }
    public void setShuffleSounds(List<String> shuffleSounds) { this.shuffleSounds = shuffleSounds; }

    public int getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(int rewardAmount) { this.rewardAmount = rewardAmount; }

    public GuiItemConfig getRewardHideItem() { return rewardHideItem; }
    public void setRewardHideItem(GuiItemConfig rewardHideItem) { this.rewardHideItem = rewardHideItem; }

    public List<String> getHideSounds() { return hideSounds; }
    public void setHideSounds(List<String> hideSounds) { this.hideSounds = hideSounds; }

    public List<String> getRevealSounds() { return revealSounds; }
    public void setRevealSounds(List<String> revealSounds) { this.revealSounds = revealSounds; }

    public int getShowRevealedItemsFor() { return showRevealedItemsFor; }
    public void setShowRevealedItemsFor(int showRevealedItemsFor) { this.showRevealedItemsFor = showRevealedItemsFor; }

    public String getGuiTitle() { return guiTitle; }
    public void setGuiTitle(String guiTitle) { this.guiTitle = guiTitle; }

    public String getGuiTitleShuffling() { return guiTitleShuffling; }
    public void setGuiTitleShuffling(String guiTitleShuffling) { this.guiTitleShuffling = guiTitleShuffling; }

    public int getGuiRows() { return guiRows; }
    public void setGuiRows(int guiRows) { this.guiRows = guiRows; }

    public String getGuiType() { return guiType; }
    public void setGuiType(String guiType) { this.guiType = guiType; }

    public GuiItemConfig getGuiFill() { return guiFill; }
    public void setGuiFill(GuiItemConfig guiFill) { this.guiFill = guiFill; }

    public int getRewardIndex() { return rewardIndex; }
    public void setRewardIndex(int rewardIndex) { this.rewardIndex = rewardIndex; }

    public List<Integer> getRewardSlots() { return rewardSlots; }
    public void setRewardSlots(List<Integer> rewardSlots) { this.rewardSlots = rewardSlots; }

    public List<Integer> getFillerSlots() { return fillerSlots; }
    public void setFillerSlots(List<Integer> fillerSlots) { this.fillerSlots = fillerSlots; }

    public Map<String, GuiItemConfig> getFillerItems() { return fillerItems; }
    public void setFillerItems(Map<String, GuiItemConfig> fillerItems) { this.fillerItems = fillerItems; }

    public GuiItemConfig getEndOfAnimationItem() { return endOfAnimationItem; }
    public void setEndOfAnimationItem(GuiItemConfig endOfAnimationItem) { this.endOfAnimationItem = endOfAnimationItem; }

    public List<Integer> getEndOfAnimationSlots() { return endOfAnimationSlots; }
    public void setEndOfAnimationSlots(List<Integer> endOfAnimationSlots) { this.endOfAnimationSlots = endOfAnimationSlots; }

    public PointerConfig getDownPointer() { return downPointer; }
    public void setDownPointer(PointerConfig downPointer) { this.downPointer = downPointer; }

    public PointerConfig getUpPointer() { return upPointer; }
    public void setUpPointer(PointerConfig upPointer) { this.upPointer = upPointer; }

    public List<String> getTickSounds() { return tickSounds; }
    public void setTickSounds(List<String> tickSounds) { this.tickSounds = tickSounds; }

    public List<String> getRewardSounds() { return rewardSounds; }
    public void setRewardSounds(List<String> rewardSounds) { this.rewardSounds = rewardSounds; }
}
