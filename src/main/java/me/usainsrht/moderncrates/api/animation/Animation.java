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

    // Scratchcard-type fields
    private int matchRequired;

    // Slot-type fields
    private Map<String, List<Integer>> slotColumns;
    private List<String> rewardWinnerColumns;
    private int rewardWinnerIndex;
    private int columnStopDelayTicks;
    private double matchChance;

    // Shared win/lose fields (used by scratchcard and slot)
    private String winTitle;
    private String loseTitle;
    private List<String> winSounds;
    private List<String> loseSounds;

    // ItemRise-type fields
    private double riseHeight;
    private int riseTicks;
    private int cycleTicks;
    private String particleType;
    private int particleCount;
    private double particleSpiralRadius;
    private double particleSpiralSpeed;
    private int blockOpenDelayTicks;
    private int riseStartDelayTicks;
    private int settleDisplayTicks;
    private List<String> riseSounds;
    private List<String> settleSounds;

    // BlockDismantle-type fields
    private String dismantleBlockType;
    private int dismantleFallDurationTicks;
    private int dismantleTopRiseDurationTicks;
    private double dismantleTopLaunchHeight;
    private double dismantleTopHorizontalRange;
    private int dismantleDisplayDurationTicks;
    private int dismantleRewardCount;
    private List<String> dismantleOpenSounds;
    private List<String> dismantleRewardSounds;
    private List<String> dismantleSettleSounds;

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

    // Scratchcard
    public int getMatchRequired() { return matchRequired; }
    public void setMatchRequired(int matchRequired) { this.matchRequired = matchRequired; }

    // Slot
    public Map<String, List<Integer>> getSlotColumns() { return slotColumns; }
    public void setSlotColumns(Map<String, List<Integer>> slotColumns) { this.slotColumns = slotColumns; }

    public List<String> getRewardWinnerColumns() { return rewardWinnerColumns; }
    public void setRewardWinnerColumns(List<String> rewardWinnerColumns) { this.rewardWinnerColumns = rewardWinnerColumns; }

    public int getRewardWinnerIndex() { return rewardWinnerIndex; }
    public void setRewardWinnerIndex(int rewardWinnerIndex) { this.rewardWinnerIndex = rewardWinnerIndex; }

    public int getColumnStopDelayTicks() { return columnStopDelayTicks; }
    public void setColumnStopDelayTicks(int columnStopDelayTicks) { this.columnStopDelayTicks = columnStopDelayTicks; }

    public double getMatchChance() { return matchChance; }
    public void setMatchChance(double matchChance) { this.matchChance = matchChance; }

    // Shared win/lose
    public String getWinTitle() { return winTitle; }
    public void setWinTitle(String winTitle) { this.winTitle = winTitle; }

    public String getLoseTitle() { return loseTitle; }
    public void setLoseTitle(String loseTitle) { this.loseTitle = loseTitle; }

    public List<String> getWinSounds() { return winSounds; }
    public void setWinSounds(List<String> winSounds) { this.winSounds = winSounds; }

    public List<String> getLoseSounds() { return loseSounds; }
    public void setLoseSounds(List<String> loseSounds) { this.loseSounds = loseSounds; }

    // ItemRise
    public double getRiseHeight() { return riseHeight; }
    public void setRiseHeight(double riseHeight) { this.riseHeight = riseHeight; }

    public int getRiseTicks() { return riseTicks; }
    public void setRiseTicks(int riseTicks) { this.riseTicks = riseTicks; }

    public int getCycleTicks() { return cycleTicks; }
    public void setCycleTicks(int cycleTicks) { this.cycleTicks = cycleTicks; }

    public String getParticleType() { return particleType; }
    public void setParticleType(String particleType) { this.particleType = particleType; }

    public int getParticleCount() { return particleCount; }
    public void setParticleCount(int particleCount) { this.particleCount = particleCount; }

    public double getParticleSpiralRadius() { return particleSpiralRadius; }
    public void setParticleSpiralRadius(double particleSpiralRadius) { this.particleSpiralRadius = particleSpiralRadius; }

    public double getParticleSpiralSpeed() { return particleSpiralSpeed; }
    public void setParticleSpiralSpeed(double particleSpiralSpeed) { this.particleSpiralSpeed = particleSpiralSpeed; }

    public int getBlockOpenDelayTicks() { return blockOpenDelayTicks; }
    public void setBlockOpenDelayTicks(int blockOpenDelayTicks) { this.blockOpenDelayTicks = blockOpenDelayTicks; }

    public int getRiseStartDelayTicks() { return riseStartDelayTicks; }
    public void setRiseStartDelayTicks(int riseStartDelayTicks) { this.riseStartDelayTicks = riseStartDelayTicks; }

    public int getSettleDisplayTicks() { return settleDisplayTicks; }
    public void setSettleDisplayTicks(int settleDisplayTicks) { this.settleDisplayTicks = settleDisplayTicks; }

    public List<String> getRiseSounds() { return riseSounds; }
    public void setRiseSounds(List<String> riseSounds) { this.riseSounds = riseSounds; }

    public List<String> getSettleSounds() { return settleSounds; }
    public void setSettleSounds(List<String> settleSounds) { this.settleSounds = settleSounds; }

    // BlockDismantle
    public String getDismantleBlockType() { return dismantleBlockType; }
    public void setDismantleBlockType(String dismantleBlockType) { this.dismantleBlockType = dismantleBlockType; }

    public int getDismantleFallDurationTicks() { return dismantleFallDurationTicks; }
    public void setDismantleFallDurationTicks(int dismantleFallDurationTicks) { this.dismantleFallDurationTicks = dismantleFallDurationTicks; }

    public int getDismantleTopRiseDurationTicks() { return dismantleTopRiseDurationTicks; }
    public void setDismantleTopRiseDurationTicks(int dismantleTopRiseDurationTicks) { this.dismantleTopRiseDurationTicks = dismantleTopRiseDurationTicks; }

    public double getDismantleTopLaunchHeight() { return dismantleTopLaunchHeight; }
    public void setDismantleTopLaunchHeight(double dismantleTopLaunchHeight) { this.dismantleTopLaunchHeight = dismantleTopLaunchHeight; }

    public double getDismantleTopHorizontalRange() { return dismantleTopHorizontalRange; }
    public void setDismantleTopHorizontalRange(double dismantleTopHorizontalRange) { this.dismantleTopHorizontalRange = dismantleTopHorizontalRange; }

    public int getDismantleDisplayDurationTicks() { return dismantleDisplayDurationTicks; }
    public void setDismantleDisplayDurationTicks(int dismantleDisplayDurationTicks) { this.dismantleDisplayDurationTicks = dismantleDisplayDurationTicks; }

    public int getDismantleRewardCount() { return dismantleRewardCount; }
    public void setDismantleRewardCount(int dismantleRewardCount) { this.dismantleRewardCount = dismantleRewardCount; }

    public List<String> getDismantleOpenSounds() { return dismantleOpenSounds; }
    public void setDismantleOpenSounds(List<String> dismantleOpenSounds) { this.dismantleOpenSounds = dismantleOpenSounds; }

    public List<String> getDismantleRewardSounds() { return dismantleRewardSounds; }
    public void setDismantleRewardSounds(List<String> dismantleRewardSounds) { this.dismantleRewardSounds = dismantleRewardSounds; }

    public List<String> getDismantleSettleSounds() { return dismantleSettleSounds; }
    public void setDismantleSettleSounds(List<String> dismantleSettleSounds) { this.dismantleSettleSounds = dismantleSettleSounds; }
}
