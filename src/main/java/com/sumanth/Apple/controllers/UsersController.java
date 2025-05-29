package com.sumanth.Apple.controllers;

import com.sumanth.Apple.models.User;
import com.sumanth.Apple.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {

	@Autowired
	UserRepository repo;

	@Autowired
	private JavaMailSender mailSender;

	@GetMapping("/")
	List<User> getUsers(HttpServletRequest req) {
		return repo.findAll();
	}

	@GetMapping("/{id}")
	User getUsers(@PathVariable Long id, HttpServletResponse res) throws Exception {
		try{
			Optional o = repo.findById(id);
			User u = (User) o.get();
			return u;
			
		}catch (Exception t){
			res.getWriter().print("{status:'ID not found'}");
			res.addHeader("Content-Type","application/json");
			res.setStatus(500);
		}
        return null;
    }

	@PostMapping("/")
	User addUser(@RequestBody User user) {
		User user1 = repo.save(user);
		return user1;
	}

	@PutMapping("/")
	User updateUser(@RequestBody User user) {
		User user1 = repo.save(user);
		return user1;
	}

	@DeleteMapping("/{id}")
	User deleteUser(@PathVariable Long id) throws Exception {
		Optional<User> a = repo.findById(id);
		User user = a.get();
		if(user != null){
			repo.deleteById(id);
			return user;
		}
		else {
			throw new Exception("Id not found");
		}
	}
	
	@PostMapping("/send-verification-link")
	public ModelAndView sendResetLink(@RequestParam("email") String email) {
	    User user = repo.findByEmail(email);
	    if (user == null) {
	        ModelAndView mv = new ModelAndView("forgot-password");
	        mv.addObject("error", "Email not found.");
	        return mv;
	    }
	    // Generate a random token
	    String token = UUID.randomUUID().toString();
	    user.setResetToken(token);
	    user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Optional: token expiry
	    repo.save(user);

	    // Send email with reset link
	    String resetLink = "http://localhost:8080/reset-password?token=" + token;
	    SimpleMailMessage message = new SimpleMailMessage();
	    message.setTo(email);
	    message.setSubject("Password Reset Request");
	    message.setText("Click the link to reset your password: " + resetLink);
	    mailSender.send(message);

	    ModelAndView mv = new ModelAndView("forgot-password");
	    mv.addObject("message", "A password reset link has been sent to your email.");
	    return mv;
	}
	
	@GetMapping("/reset-password")
	public ModelAndView showResetForm(@RequestParam("token") String token) {
	    User user = repo.findByResetToken(token);
	    if (user == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
	        ModelAndView mv = new ModelAndView("reset-password");
	        mv.addObject("error", "Invalid or expired reset link.");
	        return mv;
	    }
	    ModelAndView mv = new ModelAndView("reset-password");
	    mv.addObject("token", token);
	    return mv;
	}
	
	@PostMapping("/reset-password")
	public ModelAndView resetPassword(@RequestParam("token") String token, @RequestParam("password") String password) {
	    User user = repo.findByResetToken(token);
	    if (user == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
	        ModelAndView mv = new ModelAndView("reset-password");
	        mv.addObject("error", "Invalid or expired reset link.");
	        return mv;
	    }
	    user.setPassword(password); // Hash in production!
	    user.setResetToken(null);
	    user.setResetTokenExpiry(null);
	    repo.save(user);
	    ModelAndView mv = new ModelAndView("login");
	    mv.addObject("message", "Password reset successful. Please login.");
	    return mv;
	}

	
	@PostMapping("/signup")
	public ModelAndView registerUser(@ModelAttribute("user") User u) {
	    // In your @PostMapping("/signup") method, the @ModelAttribute("user") User u will automatically get the role if the form field name matches the entity field.
		repo.save(u);
		ModelAndView mv = new ModelAndView("login");
		mv.addObject("message", "Registration successful. Please login.");
		return mv;
	}
}
