package com.sumanth.Apple.controllers;

import com.sumanth.Apple.models.User;
import com.sumanth.Apple.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;


@Controller
public class UserViewController {

	@Autowired
	UserRepository repo;

	@Autowired
	private JavaMailSender mailSender;

	@GetMapping("/user")
	public ModelAndView userPage() {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("index");
		mv.addObject("user",new User());
		return mv;
	}

	@PostMapping("/signup")
	public ModelAndView registerUser(@ModelAttribute("user") User u) {
	    if (repo.findByEmail(u.getEmail()) != null) {
	        ModelAndView mv = new ModelAndView("index");
	        mv.addObject("error", "Email already registered.");
	        return mv;
	    }
	    u.setVerified(false);

	    // Generate OTP
	    String otp = String.valueOf(100000 + new Random().nextInt(900000));
	    u.setOtp(otp);
	    u.setOtpGeneratedTime(LocalDateTime.now());

	    repo.save(u);

	    // Send OTP email
	    SimpleMailMessage message = new SimpleMailMessage();
	    message.setTo(u.getEmail());
	    message.setSubject("Your Email Verification OTP");
	    message.setText("Your OTP for email verification is: " + otp);
	    mailSender.send(message);

	    ModelAndView mv = new ModelAndView();
	    mv.setViewName("enter_otp");
	    mv.addObject("email", u.getEmail()); // Pass email to the OTP page if needed
	    return mv;
	}

	@GetMapping("/login")
	public ModelAndView loginPage() {
		return new ModelAndView("login");
	}

	@PostMapping("/login")
	public ModelAndView loginPagSubmit(@RequestParam("username") String username,
                                   @RequestParam("password") String password) {
    User user = repo.findByUsernameAndPassword(username, password);
    ModelAndView mv = new ModelAndView();
    if (user == null) {
        mv.setViewName("login");
        mv.addObject("error", "Invalid username or password.");
    } else if (!user.isVerified()) {
        mv.setViewName("login");
        mv.addObject("error", "Please verify your email before logging in.");
    } else {
        if ("MANAGER".equalsIgnoreCase(user.getRole())) {
            mv.setViewName("manager_dashboard");
        } else if ("EMPLOYEE".equalsIgnoreCase(user.getRole())) {
            mv.setViewName("employee_dashboard");
        } else {
            mv.setViewName("login_success"); // fallback
        }
        mv.addObject("user", user);
    }
    return mv;
}

	@GetMapping("/verify")
	public ModelAndView verifyEmail(@RequestParam("email") String email) {
	    User user = repo.findByEmail(email);
	    if (user != null) {
	        user.setVerified(true);
	        repo.save(user);
	        ModelAndView mv = new ModelAndView("verify_success");
	        mv.addObject("email", email);
	        return mv;
	    } else {
	        return new ModelAndView("verify_fail");
	    }
	}

	@PostMapping("/verify-otp")
	public ModelAndView verifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
	    User user = repo.findByEmail(email);
	    if (user != null && user.getOtp() != null && user.getOtp().equals(otp)) {
	        user.setVerified(true);
	        user.setOtp(null); // clear OTP
	        repo.save(user);
	        ModelAndView mv = new ModelAndView("verify_success");
	        mv.addObject("email", email);
	        return mv;
	    } else {
	        ModelAndView mv = new ModelAndView("enter_otp");
	        mv.addObject("email", email);
	        mv.addObject("error", "Invalid OTP. Please try again.");
	        return mv;
	    }
	}

	@GetMapping("/forgot-password")
	public String forgotPasswordPage() {
	    return "forgot-password";
	}

	@PostMapping("/send-verification-link")
	public ModelAndView sendResetLink(@RequestParam("email") String email) {
	    User user = repo.findByEmail(email);
	    ModelAndView mv = new ModelAndView("forgot-password");
	    if (user == null) {
	        mv.addObject("error", "Email not found.");
	        return mv;
	    }
	    String token = UUID.randomUUID().toString();
	    user.setResetToken(token);
	    user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
	    repo.save(user);

	    // Send email
	    String resetLink = "http://localhost:8080/reset-password?token=" + token;
	    SimpleMailMessage message = new SimpleMailMessage();
	    message.setTo(email);
	    message.setSubject("Password Reset Request");
	    message.setText("Click the link to reset your password: " + resetLink);
	    mailSender.send(message);

	    mv.addObject("message", "A password reset link has been sent to your email.");
	    return mv;
	}

	@GetMapping("/reset-password")
	public ModelAndView showResetForm(@RequestParam("token") String token) {
	    User user = repo.findByResetToken(token);
	    ModelAndView mv = new ModelAndView("reset-password");
	    if (user == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
	        mv.addObject("error", "Invalid or expired reset link.");
	        return mv;
	    }
	    mv.addObject("token", token);
	    return mv;
	}

	@PostMapping("/reset-password")
	public ModelAndView resetPassword(@RequestParam("token") String token, @RequestParam("password") String password) {
	    User user = repo.findByResetToken(token);
	    ModelAndView mv = new ModelAndView("reset-password");
	    if (user == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
	        mv.addObject("error", "Invalid or expired reset link.");
	        return mv;
	    }
	    user.setPassword(password); // In production, hash the password!
	    user.setResetToken(null);
	    user.setResetTokenExpiry(null);
	    repo.save(user);
	    mv.setViewName("login");
	    mv.addObject("message", "Password reset successful. Please login.");
	    return mv;
	}

}
