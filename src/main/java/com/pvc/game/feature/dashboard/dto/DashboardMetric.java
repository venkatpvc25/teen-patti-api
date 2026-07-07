package com.pvc.game.feature.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardMetric {
    private String key;
    private String icon;
    private String title;
    private String value;
    private String label;
    private String helper;
    private String trend;
    private long rawValue;
}
