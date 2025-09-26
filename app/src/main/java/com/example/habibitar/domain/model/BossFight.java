package com.example.habibitar.domain.model;

public class BossFight {
    private String id;
    private String userId;
    private int level;
    private int bossHp;
    private int usersAttacksLeft;
    private int moneyReward;
    private String[] itemsWon;
    private boolean itemsActivated;

    public BossFight() { }

    public BossFight(BossFight other) {
        this.id = other.id;
        this.userId = other.userId;
        this.level = other.level;
        this.bossHp = other.bossHp;
        this.usersAttacksLeft = other.usersAttacksLeft;
        this.moneyReward = other.moneyReward;
        this.itemsWon = other.itemsWon;
        this.itemsActivated = other.itemsActivated;
    }

    public BossFight(String id,
                     String userId,
                     int level,
                     int bossXp,
                     int usersAttacksLeft,
                     int moneyReward,
                     String[] itemsWon,
                     boolean itemsActivated) {
        this.id = id;
        this.userId = userId;
        this.level = level;
        this.bossHp = bossXp;
        this.usersAttacksLeft = usersAttacksLeft;
        this.moneyReward = moneyReward;
        this.itemsWon = itemsWon;
        this.itemsActivated = itemsActivated;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }

    public void setUserId(String userId) { this.userId = userId; }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = level; }

    public int getBossXp() { return bossHp; }

    public void setBossXp(int bossXp) { this.bossHp = bossXp; }

    public int getUsersAttacksLeft() { return usersAttacksLeft; }

    public void setUsersAttacksLeft(int usersAttacksLeft) { this.usersAttacksLeft = usersAttacksLeft; }

    public int getMoneyReward() { return moneyReward; }

    public void setMoneyReward(int moneyReward) { this.moneyReward = moneyReward; }

    public String[] getItemsWon() { return itemsWon; }

    public void setItemsWon(String[] itemsWon) { this.itemsWon = itemsWon; }

    public boolean isItemsActivated() { return itemsActivated; }

    public void setItemsActivated(boolean itemsActivated) { this.itemsActivated = itemsActivated; }
}
