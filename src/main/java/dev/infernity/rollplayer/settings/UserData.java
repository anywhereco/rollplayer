package dev.infernity.rollplayer.settings;

import dev.infernity.rollplayer.Resources;

public class UserData {
    private static final int CURRENT_VERSION = 1;

    private final long userId;
    private int version;
    private String defaultRoll;

    public UserData(long userId) {
        this.userId = userId;
        this.version = CURRENT_VERSION;
        this.defaultRoll = "1d20";
    }

    @SuppressWarnings("unused") // Here for consistency.
    public long getUserId() {
        return userId;
    }

    public String getDefaultRoll() {
        return defaultRoll;
    }

    public void setDefaultRoll(String defaultRoll) {
        this.defaultRoll = defaultRoll;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void save() {
        Resources.getInstance().getDatabaseManager().saveUserData(this);
    }
}