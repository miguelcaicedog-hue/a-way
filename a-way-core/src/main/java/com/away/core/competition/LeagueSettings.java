package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "league_settings")
public class LeagueSettings {

    @Id
    @Column(name = "league_id")
    private UUID leagueId;

    @Column(name = "suspension_timing", nullable = false)
    private String suspensionTiming;

    @Column(name = "fair_play_yellow_points", nullable = false)
    private int fairPlayYellowPoints;

    @Column(name = "fair_play_red_points", nullable = false)
    private int fairPlayRedPoints;

    public UUID getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(UUID leagueId) {
        this.leagueId = leagueId;
    }

    public String getSuspensionTiming() {
        return suspensionTiming;
    }

    public void setSuspensionTiming(String suspensionTiming) {
        this.suspensionTiming = suspensionTiming;
    }

    public int getFairPlayYellowPoints() {
        return fairPlayYellowPoints;
    }

    public void setFairPlayYellowPoints(int fairPlayYellowPoints) {
        this.fairPlayYellowPoints = fairPlayYellowPoints;
    }

    public int getFairPlayRedPoints() {
        return fairPlayRedPoints;
    }

    public void setFairPlayRedPoints(int fairPlayRedPoints) {
        this.fairPlayRedPoints = fairPlayRedPoints;
    }
}
