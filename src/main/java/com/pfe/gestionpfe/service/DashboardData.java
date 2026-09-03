package com.pfe.gestionpfe.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardData {

    private long totalStudents;
    private long totalTeachers;
    private long totalRooms;
    private long totalDefenses;
    private long planifiedDefenses;
    private long nonPlanifiedStudents;

    private Map<String, Long> defensesByDepartment = new LinkedHashMap<>();
    private Map<String, Long> studentsByDepartment = new LinkedHashMap<>();
    private Map<String, Long> defensesByRoom       = new LinkedHashMap<>();

    private List<Map.Entry<String, Long>> teacherLoads;
    private long   maxTeacherLoad;
    private long   minTeacherLoad;
    private double balanceScore;

    // ── NOUVEAUX CHAMPS ──
    /** Nombre d'étudiants encadrés par professeur (encadrant uniquement) */
    private List<Map.Entry<String, Long>> studentsPerTeacher;

    /** Nombre de soutenances par professeur (rôle encadrant uniquement) */
    private List<Map.Entry<String, Long>> defensesByTeacher;

    /** Nombre de soutenances par filière */
    private List<Map.Entry<String, Long>> defensesByFiliere;

    // ── Getters / Setters existants ──

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getTotalTeachers() { return totalTeachers; }
    public void setTotalTeachers(long totalTeachers) { this.totalTeachers = totalTeachers; }

    public long getTotalRooms() { return totalRooms; }
    public void setTotalRooms(long totalRooms) { this.totalRooms = totalRooms; }

    public long getTotalDefenses() { return totalDefenses; }
    public void setTotalDefenses(long totalDefenses) { this.totalDefenses = totalDefenses; }

    public long getPlanifiedDefenses() { return planifiedDefenses; }
    public void setPlanifiedDefenses(long planifiedDefenses) { this.planifiedDefenses = planifiedDefenses; }

    public long getNonPlanifiedStudents() { return nonPlanifiedStudents; }
    public void setNonPlanifiedStudents(long nonPlanifiedStudents) { this.nonPlanifiedStudents = nonPlanifiedStudents; }

    public Map<String, Long> getDefensesByDepartment() { return defensesByDepartment; }
    public void setDefensesByDepartment(Map<String, Long> defensesByDepartment) { this.defensesByDepartment = defensesByDepartment; }

    public Map<String, Long> getStudentsByDepartment() { return studentsByDepartment; }
    public void setStudentsByDepartment(Map<String, Long> studentsByDepartment) { this.studentsByDepartment = studentsByDepartment; }

    public Map<String, Long> getDefensesByRoom() { return defensesByRoom; }
    public void setDefensesByRoom(Map<String, Long> defensesByRoom) { this.defensesByRoom = defensesByRoom; }

    public List<Map.Entry<String, Long>> getTeacherLoads() { return teacherLoads; }
    public void setTeacherLoads(List<Map.Entry<String, Long>> teacherLoads) { this.teacherLoads = teacherLoads; }

    public long getMaxTeacherLoad() { return maxTeacherLoad; }
    public void setMaxTeacherLoad(long maxTeacherLoad) { this.maxTeacherLoad = maxTeacherLoad; }

    public long getMinTeacherLoad() { return minTeacherLoad; }
    public void setMinTeacherLoad(long minTeacherLoad) { this.minTeacherLoad = minTeacherLoad; }

    public double getBalanceScore() { return balanceScore; }
    public void setBalanceScore(double balanceScore) { this.balanceScore = balanceScore; }

    // ── Getters / Setters nouveaux ──

    public List<Map.Entry<String, Long>> getStudentsPerTeacher() { return studentsPerTeacher; }
    public void setStudentsPerTeacher(List<Map.Entry<String, Long>> studentsPerTeacher) { this.studentsPerTeacher = studentsPerTeacher; }

    public List<Map.Entry<String, Long>> getDefensesByTeacher() { return defensesByTeacher; }
    public void setDefensesByTeacher(List<Map.Entry<String, Long>> defensesByTeacher) { this.defensesByTeacher = defensesByTeacher; }

    public List<Map.Entry<String, Long>> getDefensesByFiliere() { return defensesByFiliere; }
    public void setDefensesByFiliere(List<Map.Entry<String, Long>> defensesByFiliere) { this.defensesByFiliere = defensesByFiliere; }
}