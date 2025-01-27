package com.bankX.BankingApplication.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankX.BankingApplication.exception.AccountNotFoundException;
import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Customer;
import com.bankX.BankingApplication.service.AccountService;
import com.bankX.BankingApplication.service.CustomerService;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CustomerService customerService;

    public AccountController(AccountService accountService, CustomerService customerService) {
        this.accountService = accountService;
        this.customerService = customerService;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable Long accountId) {
        try {
            Account account = accountService.getAccountById(accountId);
            return ResponseEntity.ok(account);
        } catch (AccountNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PostMapping("/current/{customerId}")
    public ResponseEntity<Account> createCurrentAccount(@PathVariable Long customerId) {
        Customer customer = customerService.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Account currentAccount = accountService.createCurrentAccount(customer);
        return new ResponseEntity<>(currentAccount, HttpStatus.CREATED);
    }

    @PostMapping("/savings/{customerId}")
    public ResponseEntity<Account> createSavingsAccount(@PathVariable Long customerId) {
        Customer customer = customerService.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Account savingsAccount = accountService.createSavingsAccount(customer);

        // Add a joining bonus for Savings account
        savingsAccount.setBalance(savingsAccount.getBalance().add(BigDecimal.valueOf(500))); // Joining bonus for Savings
        accountService.save(savingsAccount); // Save the updated savings account
        return new ResponseEntity<>(savingsAccount, HttpStatus.CREATED);
    }

    @PostMapping("/deposit")
    public ResponseEntity<Account> deposit(@RequestParam Long fromAccountId, @RequestParam Long toAccountId, @RequestParam BigDecimal amount) {
        Account account = accountService.deposit(fromAccountId, toAccountId, amount);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Account> withdraw(@RequestParam Long accountId, @RequestParam BigDecimal amount) {
        Account updatedAccount = accountService.withdraw(accountId, amount);
        return ResponseEntity.ok(updatedAccount);
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestParam Long fromAccountId, @RequestParam Long toAccountId, @RequestParam BigDecimal amount) {
        try {
            accountService.transfer(fromAccountId, toAccountId, amount);
            return ResponseEntity.ok("Transfer successful");
        } catch (IllegalArgumentException | AccountNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/interest/{accountId}")
    public ResponseEntity<Account> applyInterest(@PathVariable Long accountId) {
        Account updatedAccount = accountService.calculateAndApplyInterest(accountId);
        return ResponseEntity.ok(updatedAccount);
    }
}
