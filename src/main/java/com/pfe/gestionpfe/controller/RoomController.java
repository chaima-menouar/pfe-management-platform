package com.pfe.gestionpfe.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.pfe.gestionpfe.model.Room;
import com.pfe.gestionpfe.service.RoomService;

@Controller
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms")
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "rooms";
    }

    @GetMapping("/rooms/new")
    public String showAddRoomForm(Model model) {
        model.addAttribute("room", new Room());
        return "add-room";
    }

    @PostMapping("/rooms/save")
    public String saveRoom(@Valid @ModelAttribute("room") Room room,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return room.getId() == null ? "add-room" : "edit-room";
        }
        try {
            roomService.saveRoom(room);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return room.getId() == null ? "add-room" : "edit-room";
        }
        return "redirect:/rooms";
    }

    @GetMapping("/rooms/edit/{id}")
    public String showEditRoomForm(@PathVariable Long id, Model model) {
        model.addAttribute("room", roomService.getRoomById(id));
        return "edit-room";
    }

    @PostMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id, Model model) {
        if (!roomService.canDeleteRoom(id)) {
            model.addAttribute("rooms", roomService.getAllRooms());
            model.addAttribute("error", "Impossible de supprimer cette salle car elle est déjà utilisée dans une soutenance.");
            return "rooms";
        }

        roomService.deleteRoom(id);
        return "redirect:/rooms";
    }
}
