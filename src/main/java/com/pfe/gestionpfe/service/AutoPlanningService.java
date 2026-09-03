package com.pfe.gestionpfe.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.pfe.gestionpfe.model.Defense;
import com.pfe.gestionpfe.model.Room;
import com.pfe.gestionpfe.model.Student;
import com.pfe.gestionpfe.model.Teacher;
import com.pfe.gestionpfe.repository.DefenseRepository;
import com.pfe.gestionpfe.repository.RoomRepository;
import com.pfe.gestionpfe.repository.StudentRepository;
import com.pfe.gestionpfe.repository.TeacherRepository;

@Service
public class AutoPlanningService {

    private static final String DEPARTEMENT_INFORMATIQUE = "Informatique";

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final DefenseRepository defenseRepository;
    private final ParametrageData parametrageData;

    public AutoPlanningService(StudentRepository studentRepository,
                               TeacherRepository teacherRepository,
                               RoomRepository roomRepository,
                               DefenseRepository defenseRepository,
                               ParametrageData parametrageData) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
        this.defenseRepository = defenseRepository;
        this.parametrageData = parametrageData;
    }

    @Transactional
    public PlanningReport generatePlanningByDepartment(String departement) {
        PlanningReport report = new PlanningReport();

        if (departement == null || !departement.equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE)) {
            report.addUnplannedMessage("Le planning automatique est disponible uniquement pour le département Informatique.");
            return report;
        }

        List<Student> students = studentRepository.findByEncadrantIsNotNull().stream()
                .filter(s -> s.getDepartement() != null
                        && s.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();

        return generatePlanningForStudents(students, null);
    }


    @Transactional
    public PlanningReport generatePlanningByFiliere(String filiere) {
        PlanningReport report = new PlanningReport();

        if (filiere == null || filiere.isBlank()) {
            report.addUnplannedMessage("Veuillez choisir une filière : GI, ID ou TDIA.");
            return report;
        }

        String filiereNorm = filiere.trim().toUpperCase();
        if (!Set.of("GI", "ID", "TDIA").contains(filiereNorm)) {
            report.addUnplannedMessage("Filière invalide. Choisissez GI, ID ou TDIA.");
            return report;
        }

        // IMPORTANT : le planning doit rester équilibré sur tout le département.
        // Donc un lancement depuis GI/ID/TDIA recalcule le planning complet
        // GI + ID + TDIA, puis l'interface affiche/exporte seulement la filière choisie.
        List<Student> students = studentRepository.findByEncadrantIsNotNull().stream()
                .filter(s -> s.getDepartement() != null
                        && s.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .toList();

        PlanningReport result = generatePlanningForStudents(students, null);
        result.addWarningMessage("Planning relancé depuis la filière " + filiereNorm
                + " : recalcul global GI + ID + TDIA pour équilibrer les charges professeurs.");
        return result;
    }

    private PlanningReport generatePlanningForStudents(List<Student> students, String filiereScope) {
        PlanningReport report = new PlanningReport();

        List<Room> rooms = roomRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Room::getNomSalle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        if (rooms.isEmpty()) {
            report.addUnplannedMessage("Aucune salle disponible.");
            return report;
        }

        if (students == null || students.isEmpty()) {
            report.addUnplannedMessage("Aucun étudiant à planifier.");
            return report;
        }

        List<LocalDate> allDates = buildAvailableDates(
                parametrageData.getDateDebut(), parametrageData.getDateFin());
        if (allDates.isEmpty()) {
            report.addUnplannedMessage("Aucune date exploitable dans la plage choisie.");
            return report;
        }

        List<LocalTime> slots = buildAllowedSlots();
        if (slots.isEmpty()) {
            report.addUnplannedMessage("Aucun créneau horaire disponible. Vérifiez les plages matin/après-midi et la durée de soutenance.");
            return report;
        }

        students = students.stream()
                .filter(s -> s.getDepartement() != null
                        && s.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .sorted(Comparator
                        .comparing((Student s) -> normalized(s.getFiliere()))
                        .thenComparing(s -> normalized(s.getNom()))
                        .thenComparing(s -> normalized(s.getPrenom())))
                .toList();

        // ═══════════════════════════════════════════════════════════════
        // CHECK CAPACITÉ BLOQUANT
        // Le planning ne doit être généré que si TOUS les étudiants peuvent
        // être placés selon le paramétrage actuel. Sinon on n'écrit RIEN
        // (aucune suppression, aucune création) et on retourne un diagnostic.
        // ═══════════════════════════════════════════════════════════════
        List<Student> eligibleStudents = students.stream()
                .filter(s -> s.getEncadrant() != null)
                .toList();

        if (eligibleStudents.isEmpty()) {
            report.addUnplannedMessage("Aucun étudiant avec encadrant à planifier. Lancez d'abord la répartition des encadrants.");
            return report;
        }

        int slotsPerDay = slots.size();
        int maxPerRoomPerDay = Math.min(slotsPerDay, parametrageData.getMaxSoutenancesRoomPerDay());
        int capacityPerDay = maxPerRoomPerDay * rooms.size();
        int totalCapacity = capacityPerDay * allDates.size();

        if (totalCapacity < eligibleStudents.size()) {
            int deficit = eligibleStudents.size() - totalCapacity;
            int neededDays = capacityPerDay > 0
                    ? (int) Math.ceil((double) deficit / capacityPerDay)
                    : 0;
            int dailyPerRoom = maxPerRoomPerDay * allDates.size();
            int neededRooms = dailyPerRoom > 0
                    ? (int) Math.ceil((double) deficit / dailyPerRoom)
                    : 0;
            int dailyPerSlot = rooms.size() * allDates.size();
            int neededSlots = dailyPerSlot > 0
                    ? (int) Math.ceil((double) deficit / dailyPerSlot)
                    : 0;

            report.addUnplannedMessage("⛔ CAPACITÉ INSUFFISANTE — planning non généré.");
            report.addUnplannedMessage("Étudiants à planifier : " + eligibleStudents.size()
                    + " | Places disponibles : " + totalCapacity
                    + " | Manque : " + deficit + " place(s).");
            report.addUnplannedMessage("Configuration actuelle : "
                    + allDates.size() + " jour(s) × "
                    + rooms.size() + " salle(s) × "
                    + maxPerRoomPerDay + " créneau(x)/salle/jour"
                    + " = " + totalCapacity + " places.");
            report.addUnplannedMessage("Solutions possibles : ajouter " + neededDays + " jour(s) "
                    + "OU " + neededRooms + " salle(s) supplémentaire(s) "
                    + "OU " + neededSlots + " créneau(x) supplémentaire(s) par jour.");
            return report;
        }

        // IMPORTANT : chaque nouvelle génération doit repartir de zéro uniquement dans son scope.
        // - Si on lance depuis une filière (GI/ID/TDIA), on supprime seulement les soutenances de cette filière.
        // - Si on lance globalement, on supprime tout le planning Informatique.
        // Les exports globaux récupèrent ensuite toutes les filières mélangées sans doublons.
        List<Defense> oldDefenses = (filiereScope == null || filiereScope.isBlank())
                ? defenseRepository.findAllInformatiqueOrdered()
                : defenseRepository.findByStudentFiliereOrderByDateSoutenanceAscHeureDebutAsc(filiereScope);
        if (!oldDefenses.isEmpty()) {
            defenseRepository.deleteAll(oldDefenses);
            defenseRepository.flush();
            report.addWarningMessage(oldDefenses.size() + " anciennes soutenances supprimées avant la nouvelle génération"
                    + (filiereScope == null || filiereScope.isBlank() ? " globale." : " de la filière " + filiereScope + "."));
        }

        Map<String, Integer> roomSlotCount = new HashMap<>();
        Map<String, List<LocalTime>> teacherDaySlots = new HashMap<>();
        Map<Long, Integer> teacherTotalLoad = new HashMap<>();
        Map<String, Integer> teacherFiliereLoad = new HashMap<>();
        Map<String, Integer> dateLoad = new HashMap<>();
        Set<Long> plannedStudents = new HashSet<>();

        // On charge les soutenances déjà existantes hors scope pour éviter les conflits
        // de salle/professeur quand on régénère seulement une filière.
        for (Defense existing : defenseRepository.findAllInformatiqueOrdered()) {
            registerExistingDefense(existing, roomSlotCount, teacherDaySlots, teacherTotalLoad, teacherFiliereLoad, dateLoad);
            if (existing.getStudent() != null && existing.getStudent().getId() != null) {
                plannedStudents.add(existing.getStudent().getId());
            }
        }

        List<Student> studentsToPlan = new ArrayList<>();
        for (Student student : students) {
            if (student.getEncadrant() == null) {
                report.addUnplannedMessage("Pas d'encadrant : " + safeStudentName(student));
                continue;
            }

            if (student.getId() != null && plannedStudents.contains(student.getId())) {
                report.addSkippedMessage("Déjà planifié dans une autre filière : " + safeStudentName(student));
                continue;
            }

            studentsToPlan.add(student);
        }

        if (studentsToPlan.isEmpty()) {
            report.addWarningMessage("Aucun nouvel étudiant à planifier.");
            return report;
        }

        // IMPORTANT : avant كان كيرتب الطلبة بالفilière، لذلك كيخطط GI اللولين ويبقاو ID/TDIA.
        // دابا كنخلطهم round-robin حسب filière باش التوزيع بين filières يكون عادل.
        studentsToPlan = balanceStudentsByFiliere(studentsToPlan);

        int perDayCapacity = Math.max(1,
                Math.min(slots.size(), parametrageData.getMaxSoutenancesRoomPerDay()) * rooms.size());
        int computedDays = (int) Math.ceil((double) studentsToPlan.size() / perDayCapacity);
        int minDays = Math.max(1, parametrageData.getMinJoursPlanning());

        // Le check de capacité bloquant a déjà été fait plus haut (avant la suppression
        // des anciennes soutenances). Ici on calcule juste le nombre de jours à utiliser.
        int targetDays = Math.min(Math.max(minDays, computedDays), allDates.size());
        List<LocalDate> candidateDates = new ArrayList<>(allDates.subList(0, targetDays));

        int activeJuryTeachers = Math.max(1, (int) teacherRepository.findByDisponibleJuryTrueAndActifTrue().stream()
                .filter(t -> t.getDepartement() != null
                        && t.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .count());
        int targetTeacherLoadHigh = Math.max(1, (int) Math.ceil((studentsToPlan.size() * 3.0) / activeJuryTeachers));
        report.addWarningMessage("Objectif équilibrage soutenances/professeur : charge cible "
                + targetTeacherLoadHigh + " ou " + Math.max(0, targetTeacherLoadHigh - 1)
                + " participations.");

        for (Student student : studentsToPlan) {
            if (plannedStudents.contains(student.getId())) {
                report.addSkippedMessage("Déjà planifié (session) : " + safeStudentName(student));
                continue;
            }

            BestPlan bestPlan = findBestPlanForStudent(
                    student, rooms, candidateDates, slots,
                    roomSlotCount, teacherDaySlots, teacherTotalLoad,
                    teacherFiliereLoad, dateLoad, targetTeacherLoadHigh);

            if (bestPlan == null) {
                report.addUnplannedMessage("Impossible à planifier : " + safeStudentName(student));
                continue;
            }

            Defense d = new Defense();
            d.setStudent(student);
            d.setEncadrant(student.getEncadrant());
            d.setJury1(bestPlan.jury1());
            d.setJury2(bestPlan.jury2());
            d.setRoom(bestPlan.room());
            d.setDateSoutenance(bestPlan.date());
            d.setHeureDebut(bestPlan.start());
            d.setHeureFin(bestPlan.start().plusMinutes(parametrageData.getDureeSoutenanceMinutes()));
            d.setLangue(student.getLangueSoutenance() != null ? student.getLangueSoutenance() : "FR");
            d.setStatut("PLANIFIEE");
            defenseRepository.save(d);

            String roomKey = roomSlotKey(bestPlan.room().getId(), bestPlan.date(), bestPlan.start());
            roomSlotCount.merge(roomKey, 1, Integer::sum);
            dateLoad.merge(bestPlan.date().toString(), 1, Integer::sum);

            registerTeacherSlot(teacherDaySlots, student.getEncadrant(), bestPlan.date(), bestPlan.start());
            registerTeacherSlot(teacherDaySlots, bestPlan.jury1(), bestPlan.date(), bestPlan.start());
            registerTeacherSlot(teacherDaySlots, bestPlan.jury2(), bestPlan.date(), bestPlan.start());

            incrementTeacherLoad(teacherTotalLoad, student.getEncadrant());
            incrementTeacherLoad(teacherTotalLoad, bestPlan.jury1());
            incrementTeacherLoad(teacherTotalLoad, bestPlan.jury2());

            incrementTeacherFiliereLoad(teacherFiliereLoad, student.getEncadrant(), student.getFiliere());
            incrementTeacherFiliereLoad(teacherFiliereLoad, bestPlan.jury1(), student.getFiliere());
            incrementTeacherFiliereLoad(teacherFiliereLoad, bestPlan.jury2(), student.getFiliere());

            plannedStudents.add(student.getId());

            report.addPlannedMessage(
                    safeStudentName(student)
                            + " -> " + bestPlan.date()
                            + " " + bestPlan.start()
                            + " | salle=" + bestPlan.room().getNomSalle()
                            + " | jury=" + fullName(bestPlan.jury1())
                            + ", " + fullName(bestPlan.jury2())
                            + " | filière=" + (student.getFiliere() == null ? "" : student.getFiliere())
            );
        }

        // ═══════════════════════════════════════════════════════════════
        // ROLLBACK SI INCOMPLET
        // Même quand la capacité brute est suffisante, des contraintes
        // (jury, langue, disponibilités) peuvent empêcher certains
        // étudiants d'être placés. Dans ce cas on rollback la transaction
        // → ni l'ancien planning ne disparaît, ni de planning partiel n'est créé.
        // ═══════════════════════════════════════════════════════════════
        Set<Long> targetIds = new HashSet<>();
        for (Student s : studentsToPlan) {
            if (s.getId() != null) {
                targetIds.add(s.getId());
            }
        }

        int planifies = 0;
        for (Long id : plannedStudents) {
            if (targetIds.contains(id)) {
                planifies++;
            }
        }

        if (planifies < studentsToPlan.size()) {
            int manquants = studentsToPlan.size() - planifies;
            report.addUnplannedMessage("⛔ PLANNING NON GÉNÉRÉ : " + manquants
                    + " étudiant(s) n'ont pas pu être placés malgré la capacité disponible. "
                    + "Causes possibles : contraintes de jury, de langue ou de disponibilité. "
                    + "Aucune soutenance n'a été enregistrée (annulation transactionnelle).");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return report;
    }


    private void registerExistingDefense(Defense d,
                                         Map<String, Integer> roomSlotCount,
                                         Map<String, List<LocalTime>> teacherDaySlots,
                                         Map<Long, Integer> teacherTotalLoad,
                                         Map<String, Integer> teacherFiliereLoad,
                                         Map<String, Integer> dateLoad) {
        if (d == null || d.getDateSoutenance() == null || d.getHeureDebut() == null) return;

        if (d.getRoom() != null && d.getRoom().getId() != null) {
            roomSlotCount.merge(roomSlotKey(d.getRoom().getId(), d.getDateSoutenance(), d.getHeureDebut()), 1, Integer::sum);
        }
        dateLoad.merge(d.getDateSoutenance().toString(), 1, Integer::sum);

        String filiere = d.getStudent() != null ? d.getStudent().getFiliere() : null;
        registerTeacherSlot(teacherDaySlots, d.getEncadrant(), d.getDateSoutenance(), d.getHeureDebut());
        registerTeacherSlot(teacherDaySlots, d.getJury1(), d.getDateSoutenance(), d.getHeureDebut());
        registerTeacherSlot(teacherDaySlots, d.getJury2(), d.getDateSoutenance(), d.getHeureDebut());

        incrementTeacherLoad(teacherTotalLoad, d.getEncadrant());
        incrementTeacherLoad(teacherTotalLoad, d.getJury1());
        incrementTeacherLoad(teacherTotalLoad, d.getJury2());

        incrementTeacherFiliereLoad(teacherFiliereLoad, d.getEncadrant(), filiere);
        incrementTeacherFiliereLoad(teacherFiliereLoad, d.getJury1(), filiere);
        incrementTeacherFiliereLoad(teacherFiliereLoad, d.getJury2(), filiere);
    }

    private BestPlan findBestPlanForStudent(Student student,
                                            List<Room> rooms,
                                            List<LocalDate> dates,
                                            List<LocalTime> slots,
                                            Map<String, Integer> roomSlotCount,
                                            Map<String, List<LocalTime>> teacherDaySlots,
                                            Map<Long, Integer> teacherTotalLoad,
                                            Map<String, Integer> teacherFiliereLoad,
                                            Map<String, Integer> dateLoad,
                                            int targetTeacherLoadHigh) {
        BestPlan best = null;

        for (LocalDate date : dates) {
            for (LocalTime start : slots) {
                for (Room room : findAvailableRooms(rooms, date, start, roomSlotCount)) {
                    List<Teacher> candidates = findValidJuryCandidates(
                            student, date, start, teacherDaySlots, teacherTotalLoad, teacherFiliereLoad,
                            targetTeacherLoadHigh, true);
                    if (candidates.size() < 2) {
                        candidates = findValidJuryCandidates(
                                student, date, start, teacherDaySlots, teacherTotalLoad, teacherFiliereLoad,
                                targetTeacherLoadHigh, false);
                    }
                    if (candidates.size() < 2) continue;

                    int limit = Math.min(candidates.size(), 32);
                    for (int i = 0; i < limit; i++) {
                        for (int j = i + 1; j < limit; j++) {
                            Teacher jury1 = candidates.get(i);
                            Teacher jury2 = candidates.get(j);
                            if (jury1.getId().equals(jury2.getId())) continue;

                            int score = scorePlan(student, jury1, jury2, room, date, start,
                                    roomSlotCount, teacherDaySlots, teacherTotalLoad,
                                    teacherFiliereLoad, dateLoad);

                            BestPlan current = new BestPlan(date, start, room, jury1, jury2, score);
                            if (best == null || current.score() > best.score()) {
                                best = current;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    private List<Room> findAvailableRooms(List<Room> rooms,
                                          LocalDate date,
                                          LocalTime start,
                                          Map<String, Integer> roomSlotCount) {
        List<Room> available = new ArrayList<>();
        for (Room room : rooms) {
            int dayCount = getDayRoomCount(roomSlotCount, room.getId(), date);
            if (dayCount >= parametrageData.getMaxSoutenancesRoomPerDay()) continue;

            String slotKey = roomSlotKey(room.getId(), date, start);
            if (roomSlotCount.getOrDefault(slotKey, 0) == 0) {
                available.add(room);
            }
        }

        available.sort(Comparator
                .comparingInt((Room r) -> getDayRoomCount(roomSlotCount, r.getId(), date))
                .thenComparing(Room::getNomSalle, Comparator.nullsLast(String::compareToIgnoreCase)));
        return available;
    }

    private List<Teacher> findValidJuryCandidates(Student student,
                                                  LocalDate date,
                                                  LocalTime start,
                                                  Map<String, List<LocalTime>> teacherDaySlots,
                                                  Map<Long, Integer> teacherTotalLoad,
                                                  Map<String, Integer> teacherFiliereLoad,
                                                  int targetTeacherLoadHigh,
                                                  boolean enforceQuota) {
        List<Teacher> teachers = teacherRepository
                .findByDisponibleJuryTrueAndActifTrue().stream()
                .filter(t -> t.getDepartement() != null
                        && t.getDepartement().equalsIgnoreCase(DEPARTEMENT_INFORMATIQUE))
                .filter(t -> student.getEncadrant() == null || !t.getId().equals(student.getEncadrant().getId()))
                .filter(t -> isAvailableInMemory(t, date, start, teacherDaySlots))
                .filter(t -> isLanguageOk(t, student.getLangueSoutenance()))
                .filter(t -> !enforceQuota || getTeacherTotalLoad(t.getId(), teacherTotalLoad) < targetTeacherLoadHigh)
                .collect(Collectors.toCollection(ArrayList::new));

        teachers.sort(Comparator
                .comparingInt((Teacher t) -> getTeacherTotalLoad(t.getId(), teacherTotalLoad))
                .thenComparingInt(t -> getTeacherDayLoad(t.getId(), date, teacherDaySlots))
                .thenComparingInt(t -> getTeacherFiliereLoad(t.getId(), student.getFiliere(), teacherFiliereLoad))
                .thenComparing(t -> normalized(t.getNom()))
                .thenComparing(t -> normalized(t.getPrenom())));

        return teachers;
    }

    private boolean isAvailableInMemory(Teacher teacher,
                                        LocalDate date,
                                        LocalTime start,
                                        Map<String, List<LocalTime>> teacherDaySlots) {
        String key = teacherDayKey(teacher.getId(), date);
        List<LocalTime> reserved = teacherDaySlots.getOrDefault(key, List.of());

        if (reserved.size() >= parametrageData.getMaxSoutenancesTeacherPerDay()) return false;

        LocalTime proposedEnd = start.plusMinutes(parametrageData.getDureeSoutenanceMinutes());
        int pauseMinutes = parametrageData.getDureeReposMinutes();

        for (LocalTime existing : reserved) {
            LocalTime buffStart = existing.minusMinutes(pauseMinutes);
            LocalTime buffEnd = existing.plusMinutes(parametrageData.getDureeSoutenanceMinutes())
                    .plusMinutes(pauseMinutes);
            if (overlaps(start, proposedEnd, buffStart, buffEnd)) return false;
        }

        return true;
    }

    private int scorePlan(Student student, Teacher jury1, Teacher jury2, Room room,
                          LocalDate date, LocalTime start,
                          Map<String, Integer> roomSlotCount,
                          Map<String, List<LocalTime>> teacherDaySlots,
                          Map<Long, Integer> teacherTotalLoad,
                          Map<String, Integer> teacherFiliereLoad,
                          Map<String, Integer> dateLoad) {
        int score = 0;

        score += scoreTeacherForStudent(student, jury1, date, teacherDaySlots, teacherTotalLoad, teacherFiliereLoad);
        score += scoreTeacherForStudent(student, jury2, date, teacherDaySlots, teacherTotalLoad, teacherFiliereLoad);

        int j1Total = getTeacherTotalLoad(jury1.getId(), teacherTotalLoad);
        int j2Total = getTeacherTotalLoad(jury2.getId(), teacherTotalLoad);
        int j1Day = getTeacherDayLoad(jury1.getId(), date, teacherDaySlots);
        int j2Day = getTeacherDayLoad(jury2.getId(), date, teacherDaySlots);
        int roomDay = getDayRoomCount(roomSlotCount, room.getId(), date);
        int dateTotal = dateLoad.getOrDefault(date.toString(), 0);

        // أهم حاجة: charge globale متوازنة بين الأساتذة
        score -= (j1Total + j2Total) * 80;

        // Pénalité légère sur charge du jour - réduite pour ne pas bloquer l'après-midi
        score -= (j1Day + j2Day) * 30;

        // Bonus si créneau dans même demi-journée que soutenances existantes du prof
        boolean isAfternoon = start.getHour() >= 14;
        List<LocalTime> j1Slots = teacherDaySlots.getOrDefault(teacherDayKey(jury1.getId(), date), List.of());
        List<LocalTime> j2Slots = teacherDaySlots.getOrDefault(teacherDayKey(jury2.getId(), date), List.of());
        long j1SameHalf = j1Slots.stream().filter(t -> (t.getHour() >= 14) == isAfternoon).count();
        long j2SameHalf = j2Slots.stream().filter(t -> (t.getHour() >= 14) == isAfternoon).count();
        score += (j1SameHalf + j2SameHalf) * 15;

        // توزيع نفس filière على الأساتذة كذلك يكون équilibré
        score -= getTeacherFiliereLoad(jury1.getId(), student.getFiliere(), teacherFiliereLoad) * 45;
        score -= getTeacherFiliereLoad(jury2.getId(), student.getFiliere(), teacherFiliereLoad) * 45;

        // توزيع السوتنانسات على الأيام والقاعات
        score -= roomDay * 20;
        score -= dateTotal * 4;

        // تشجيع توافق spécialité ولكن ماشي على حساب التوازن
        if (same(student.getSpecialite(), jury1.getSpecialite())) score += 15;
        if (same(student.getSpecialite(), jury2.getSpecialite())) score += 15;

        if (same(student.getLangueSoutenance(), jury1.getLangue())) score += 8;
        if (same(student.getLangueSoutenance(), jury2.getLangue())) score += 8;

        // اختيار كرينوات مريحة: 9/11/14/16 مناسبة مع pause 1h

        return score;
    }

    private int scoreTeacherForStudent(Student student, Teacher teacher,
                                       LocalDate date,
                                       Map<String, List<LocalTime>> teacherDaySlots,
                                       Map<Long, Integer> teacherTotalLoad,
                                       Map<String, Integer> teacherFiliereLoad) {
        int score = 0;
        score -= getTeacherTotalLoad(teacher.getId(), teacherTotalLoad) * 50;
        score -= getTeacherDayLoad(teacher.getId(), date, teacherDaySlots) * 90;
        score -= getTeacherFiliereLoad(teacher.getId(), student.getFiliere(), teacherFiliereLoad) * 35;

        if (same(student.getSpecialite(), teacher.getSpecialite())) score += 12;
        if (same(student.getLangueSoutenance(), teacher.getLangue())) score += 8;

        return score;
    }

    private List<Student> balanceStudentsByFiliere(List<Student> students) {
        Map<String, Queue<Student>> byFiliere = new TreeMap<>();
        for (Student s : students) {
            String key = normalized(s.getFiliere());
            byFiliere.computeIfAbsent(key, k -> new LinkedList<>()).add(s);
        }

        List<Student> balanced = new ArrayList<>();
        boolean added;
        do {
            added = false;
            for (Queue<Student> queue : byFiliere.values()) {
                Student s = queue.poll();
                if (s != null) {
                    balanced.add(s);
                    added = true;
                }
            }
        } while (added);
        return balanced;
    }

    private List<LocalDate> buildAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) return dates;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (Boolean.TRUE.equals(parametrageData.isExclureWeekend()) && isWeekend(date)) continue;
            dates.add(date);
        }
        return dates;
    }

    private List<LocalTime> buildAllowedSlots() {
        return parametrageData.getWorkingSlots().stream().sorted().toList();
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean isLanguageOk(Teacher teacher, String lang) {
        if (lang == null || lang.isBlank()) return true;

        if (Boolean.TRUE.equals(parametrageData.isExigerProfLangue())) {
            return Boolean.TRUE.equals(teacher.getEstProfLangue())
                    && teacher.getLangue() != null
                    && teacher.getLangue().equalsIgnoreCase(lang);
        }

        if (Boolean.TRUE.equals(teacher.getEstProfLangue())) {
            return teacher.getLangue() != null
                    && teacher.getLangue().equalsIgnoreCase(lang);
        }
        return true;
    }

    private boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        if (s1 == null || e1 == null || s2 == null || e2 == null) return false;
        return s1.isBefore(e2) && e1.isAfter(s2);
    }

    private String roomSlotKey(Long roomId, LocalDate date, LocalTime start) {
        return "R" + roomId + "_" + date + "_" + start;
    }

    private String teacherDayKey(Long teacherId, LocalDate date) {
        return "T" + teacherId + "_" + date;
    }

    private String teacherFiliereKey(Long teacherId, String filiere) {
        return "T" + teacherId + "_F_" + normalized(filiere);
    }

    private void registerTeacherSlot(Map<String, List<LocalTime>> map,
                                     Teacher teacher,
                                     LocalDate date,
                                     LocalTime start) {
        if (teacher == null || teacher.getId() == null || date == null || start == null) return;
        String key = teacherDayKey(teacher.getId(), date);
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(start);
    }

    private int getDayRoomCount(Map<String, Integer> roomSlotCount,
                                Long roomId, LocalDate date) {
        return roomSlotCount.entrySet().stream()
                .filter(e -> e.getKey().startsWith("R" + roomId + "_" + date + "_"))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    private int getTeacherDayLoad(Long teacherId, LocalDate date,
                                  Map<String, List<LocalTime>> teacherDaySlots) {
        if (teacherId == null || date == null) return 0;
        return teacherDaySlots.getOrDefault(teacherDayKey(teacherId, date), List.of()).size();
    }

    private void incrementTeacherLoad(Map<Long, Integer> teacherTotalLoad, Teacher teacher) {
        if (teacher == null || teacher.getId() == null) return;
        teacherTotalLoad.merge(teacher.getId(), 1, Integer::sum);
    }

    private int getTeacherTotalLoad(Long teacherId, Map<Long, Integer> teacherTotalLoad) {
        if (teacherId == null) return 0;
        return teacherTotalLoad.getOrDefault(teacherId, 0);
    }

    private void incrementTeacherFiliereLoad(Map<String, Integer> teacherFiliereLoad,
                                             Teacher teacher,
                                             String filiere) {
        if (teacher == null || teacher.getId() == null) return;
        teacherFiliereLoad.merge(teacherFiliereKey(teacher.getId(), filiere), 1, Integer::sum);
    }

    private int getTeacherFiliereLoad(Long teacherId, String filiere,
                                      Map<String, Integer> teacherFiliereLoad) {
        if (teacherId == null) return 0;
        return teacherFiliereLoad.getOrDefault(teacherFiliereKey(teacherId, filiere), 0);
    }

    private boolean same(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String safeStudentName(Student student) {
        String full = ((student.getNom() == null ? "" : student.getNom())
                + " " + (student.getPrenom() == null ? "" : student.getPrenom())).trim();
        return full.isBlank() ? "Etudiant #" + student.getId() : full;
    }

    private String fullName(Teacher teacher) {
        String full = ((teacher.getNom() == null ? "" : teacher.getNom())
                + " " + (teacher.getPrenom() == null ? "" : teacher.getPrenom())).trim();
        return full.isBlank() ? "Prof #" + teacher.getId() : full;
    }

    private record BestPlan(
            LocalDate date,
            LocalTime start,
            Room room,
            Teacher jury1,
            Teacher jury2,
            int score
    ) {}
}