package vitbuk.com.Ambotorix.entities;

import java.util.Objects;

public class Leader {
    String fullName;
    String shortName;
    String description;
    String picPath;

    public Leader(String fullName, String shortName, String description, String picPath) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.description = description;
        this.picPath = picPath;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leader leader = (Leader) o;
        return fullName.equals(leader.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }
}