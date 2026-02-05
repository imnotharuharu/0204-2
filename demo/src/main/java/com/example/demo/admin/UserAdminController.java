package com.example.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        AppUser user = userRepository.findById(id).orElseThrow();
        model.addAttribute("user", user);
        return "admin/user-edit";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @RequestParam String role,
        @RequestParam(defaultValue = "false") boolean enabled,
        RedirectAttributes redirectAttributes
    ) {
        AppUser user = userRepository.findById(id).orElseThrow();
        user.setRole(role);
        user.setEnabled(enabled);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "ユーザーを更新しました");
        return "redirect:/admin/users";
    }
}
