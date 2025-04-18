package com.finance.accountmanagement.controller;

import com.finance.accountmanagement.entity.ChildAccount;
import com.finance.accountmanagement.entity.ParentAccount;
import com.finance.accountmanagement.repository.ChildAccountRepository;
import com.finance.accountmanagement.repository.ParentAccountRepository;
import com.finance.accountmanagement.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final ParentAccountRepository parentAccountRepository;
    private final ChildAccountRepository childAccountRepository;

    public TransactionController(ParentAccountRepository parentAccountRepository,
                               ChildAccountRepository childAccountRepository) {
        this.parentAccountRepository = parentAccountRepository;
        this.childAccountRepository = childAccountRepository;
    }

    @PostMapping("/parent-to-child")
    public ResponseEntity<?> transferParentToChild(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam UUID childId,
            @RequestParam BigDecimal amount) {
        
        if (!"PARENT".equals(userDetails.getAccountType())) {
            return ResponseEntity.badRequest().body("Only parent accounts can perform this operation");
        }

        ParentAccount parentAccount = parentAccountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Parent account not found"));

        ChildAccount childAccount = childAccountRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child account not found"));

        if (!childAccount.getParentAccount().getId().equals(parentAccount.getId())) {
            return ResponseEntity.badRequest().body("Child account does not belong to this parent");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive");
        }

        if (parentAccount.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Insufficient funds");
        }

        parentAccount.setBalance(parentAccount.getBalance().subtract(amount));
        childAccount.setBalance(childAccount.getBalance().add(amount));

        parentAccountRepository.save(parentAccount);
        childAccountRepository.save(childAccount);

        return ResponseEntity.ok("Transfer successful");
    }

    @PostMapping("/child-to-parent")
    public ResponseEntity<?> transferChildToParent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam BigDecimal amount) {
        
        if (!"CHILD".equals(userDetails.getAccountType())) {
            return ResponseEntity.badRequest().body("Only child accounts can perform this operation");
        }

        ChildAccount childAccount = childAccountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Child account not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive");
        }

        if (childAccount.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Insufficient funds");
        }

        ParentAccount parentAccount = childAccount.getParentAccount();
        childAccount.setBalance(childAccount.getBalance().subtract(amount));
        parentAccount.setBalance(parentAccount.getBalance().add(amount));

        childAccountRepository.save(childAccount);
        parentAccountRepository.save(parentAccount);

        return ResponseEntity.ok("Transfer successful");
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam BigDecimal amount) {
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive");
        }

        if ("PARENT".equals(userDetails.getAccountType())) {
            ParentAccount parentAccount = parentAccountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Parent account not found"));
            parentAccount.setBalance(parentAccount.getBalance().add(amount));
            parentAccountRepository.save(parentAccount);
        } else {
            ChildAccount childAccount = childAccountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Child account not found"));
            childAccount.setBalance(childAccount.getBalance().add(amount));
            childAccountRepository.save(childAccount);
        }

        return ResponseEntity.ok("Deposit successful");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam BigDecimal amount) {
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive");
        }

        if ("PARENT".equals(userDetails.getAccountType())) {
            ParentAccount parentAccount = parentAccountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Parent account not found"));

            if (parentAccount.getBalance().compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body("Insufficient funds");
            }

            parentAccount.setBalance(parentAccount.getBalance().subtract(amount));
            parentAccountRepository.save(parentAccount);
        } else {
            ChildAccount childAccount = childAccountRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Child account not found"));

            if (childAccount.getBalance().compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body("Insufficient funds");
            }

            childAccount.setBalance(childAccount.getBalance().subtract(amount));
            childAccountRepository.save(childAccount);
        }

        return ResponseEntity.ok("Withdrawal successful");
    }
} 