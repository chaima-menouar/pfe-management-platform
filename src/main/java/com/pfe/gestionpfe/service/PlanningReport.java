package com.pfe.gestionpfe.service;

import java.util.ArrayList;
import java.util.List;

public class PlanningReport {

    private int plannedCount;
    private int unplannedCount;
    private int skippedCount;
    private int warningCount;

    private List<String> plannedMessages;
    private List<String> unplannedMessages;
    private List<String> skippedMessages;
    private List<String> warningMessages;

    public PlanningReport() {
        this.plannedMessages = new ArrayList<>();
        this.unplannedMessages = new ArrayList<>();
        this.skippedMessages = new ArrayList<>();
        this.warningMessages = new ArrayList<>();
    }

    public int getPlannedCount() {
        return plannedCount;
    }

    public void setPlannedCount(int plannedCount) {
        this.plannedCount = plannedCount;
    }

    public int getUnplannedCount() {
        return unplannedCount;
    }

    public void setUnplannedCount(int unplannedCount) {
        this.unplannedCount = unplannedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public List<String> getPlannedMessages() {
        return plannedMessages;
    }

    public void setPlannedMessages(List<String> plannedMessages) {
        this.plannedMessages = plannedMessages != null ? plannedMessages : new ArrayList<>();
        this.plannedCount = this.plannedMessages.size();
    }

    public List<String> getUnplannedMessages() {
        return unplannedMessages;
    }

    public void setUnplannedMessages(List<String> unplannedMessages) {
        this.unplannedMessages = unplannedMessages != null ? unplannedMessages : new ArrayList<>();
        this.unplannedCount = this.unplannedMessages.size();
    }

    public List<String> getSkippedMessages() {
        return skippedMessages;
    }

    public void setSkippedMessages(List<String> skippedMessages) {
        this.skippedMessages = skippedMessages != null ? skippedMessages : new ArrayList<>();
        this.skippedCount = this.skippedMessages.size();
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }

    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages != null ? warningMessages : new ArrayList<>();
        this.warningCount = this.warningMessages.size();
    }

    public void addPlannedMessage(String message) {
        this.plannedMessages.add(message);
        this.plannedCount++;
    }

    public void addUnplannedMessage(String message) {
        this.unplannedMessages.add(message);
        this.unplannedCount++;
    }

    public void addSkippedMessage(String message) {
        this.skippedMessages.add(message);
        this.skippedCount++;
    }

    public void addWarningMessage(String message) {
        this.warningMessages.add(message);
        this.warningCount++;
    }

    public boolean hasErrors() {
        return unplannedCount > 0;
    }

    public boolean hasWarnings() {
        return warningCount > 0;
    }

    public boolean hasPlannedMessages() {
        return !plannedMessages.isEmpty();
    }

    public boolean hasUnplannedMessages() {
        return !unplannedMessages.isEmpty();
    }

    public boolean hasSkippedMessages() {
        return !skippedMessages.isEmpty();
    }

    public boolean hasWarningMessages() {
        return !warningMessages.isEmpty();
    }

    public boolean hasAnyMessage() {
        return hasPlannedMessages()
                || hasUnplannedMessages()
                || hasSkippedMessages()
                || hasWarningMessages();
    }

    public int getTotalMessages() {
        return plannedCount + unplannedCount + skippedCount + warningCount;
    }
}