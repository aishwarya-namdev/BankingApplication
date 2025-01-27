package com.bankX.BankingApplication.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.bankX.BankingApplication.exception.AccountNotFoundException;
import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Customer;
import com.bankX.BankingApplication.model.Transaction;
import com.bankX.BankingApplication.repository.AccountRepository;
import com.bankX.BankingApplication.repository.TransactionRepository;
import jakarta.transaction.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    @Autowired 
    private final NotificationService notificationService;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository, NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }
    @Transactional
    public Account save(Account account) {
        return accountRepository.saveAndFlush(account);
    }

    // Method to create a savings account
    @Transactional
    public Account createSavingsAccount(Customer customer) {
        // Create a new Savings account for the customer
        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setBalance(BigDecimal.valueOf(500));  // Joining bonus of 500
        
        // Save the account
        return accountRepository.save(account);
    }

    @Transactional
    public Account createCurrentAccount(Customer customer) {
        // Create a new Current account for the customer
        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountType(Account.AccountType.CURRENT);
        account.setBalance(BigDecimal.ZERO);  // Starting balance for current account

        System.out.println("Account before saving: " + account);  // Logging to inspect the object

        try {
            // Save the account
            Account savedAccount = accountRepository.save(account);

            // Explicitly flush if necessary (depending on your JPA configuration)
            accountRepository.flush();

            System.out.println("Saved Account: " + savedAccount);  // Logging the saved account
            return savedAccount;
        } catch (DataAccessException e) {
            System.err.println("Error while saving account: " + e.getMessage());
            e.printStackTrace();  // Print the full stack trace for debugging
            return null;
        } catch (Exception e) {
            // Handle other unexpected exceptions here (optional)
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public Account getAccountById(Long accountId) throws AccountNotFoundException {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found"));
    }
    
    public Account deposit(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Fetch the source account (where the money is debited from)
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("From account not found"));
    
        // Fetch the destination account (where the money is credited to)
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("To account not found"));
    
        // Check if the source account has sufficient balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
    
        // Check for duplicate transaction
        Transaction existingTransaction = transactionRepository
                .findByAccountAndAmountAndDescription(fromAccount, amount, "Deposit to Account ID: " + toAccountId);
        if (existingTransaction != null) {
            throw new RuntimeException("Duplicate transaction detected");
        }
    
        // Deduct the amount from the source account
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    
        // Add the amount to the destination account
        toAccount.setBalance(toAccount.getBalance().add(amount));
    
        // Create a transaction record for the debit from the source account
        Transaction debitTransaction = new Transaction();
        debitTransaction.setAccount(fromAccount);
        debitTransaction.setAmount(amount.negate()); // Negative amount to indicate debit
        debitTransaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        debitTransaction.setDescription("Transfer to Account ID: " + toAccountId);
        transactionRepository.save(debitTransaction);
    
        // Create a transaction record for the credit to the destination account
        Transaction creditTransaction = new Transaction();
        creditTransaction.setAccount(toAccount);
        creditTransaction.setAmount(amount);
        creditTransaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        creditTransaction.setDescription("Transfer from Account ID: " + fromAccountId);
        transactionRepository.save(creditTransaction);
    
        // Save the updated account balances
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    
        return toAccount; // Optionally return the updated destination account
    }
    

    public Account withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Transaction fee (0.05%)
        BigDecimal transactionFee = amount.multiply(BigDecimal.valueOf(0.0005));
        account.setBalance(account.getBalance().subtract(amount.add(transactionFee)));

        // Withdrawal transaction
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        transaction.setDescription("Withdrawal");
        transactionRepository.save(transaction);

        notificationService.sendEmailNotification(
            account.getCustomer(),
            "Withdrawal Notification",
            "Amount of " + amount + " withdrawn. Transaction fee of " + transactionFee + " applied."
        );

        return accountRepository.save(account);
    }

    public Account transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow(() -> new RuntimeException("From account not found"));
        Account toAccount = accountRepository.findById(toAccountId).orElseThrow(() -> new RuntimeException("To account not found"));

        // Ensure transfer is only from Current Account
        if (fromAccount.getAccountType() != Account.AccountType.CURRENT) {
            throw new RuntimeException("Transfers are allowed only from Current Accounts");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        BigDecimal transactionFee = amount.multiply(BigDecimal.valueOf(0.0005)); // 0.05% fee
        fromAccount.setBalance(fromAccount.getBalance().subtract(transactionFee));

        // Debit and credit transactions
        Transaction fromTransaction = new Transaction();
        fromTransaction.setAccount(fromAccount);
        fromTransaction.setAmount(amount.add(transactionFee).negate()); // Include fee
        fromTransaction.setTransactionType(Transaction.TransactionType.TRANSFER);
        fromTransaction.setDescription("Transfer to Account ID: " + toAccountId);
        transactionRepository.save(fromTransaction);

        Transaction toTransaction = new Transaction();
        toTransaction.setAccount(toAccount);
        toTransaction.setAmount(amount);
        toTransaction.setTransactionType(Transaction.TransactionType.TRANSFER);
        toTransaction.setDescription("Transfer from Account ID: " + fromAccountId);
        transactionRepository.save(toTransaction);

        // Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Notifications
        notificationService.sendEmailNotification(
            fromAccount.getCustomer(),
            "Transfer Notification",
            "Amount of " + amount + " transferred to Account ID: " + toAccountId
        );
        notificationService.sendEmailNotification(
            toAccount.getCustomer(),
            "Transfer Notification",
            "Amount of " + amount + " received from Account ID: " + fromAccountId
        );

        return toAccount;
    }

    // Interest Calculation for Savings Account
    public Account calculateAndApplyInterest(Long accountId) {
        // Fetch account details
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + accountId + " not found"));
    
        // Apply interest only for Savings accounts
        if (account.getAccountType() != Account.AccountType.SAVINGS) {
            throw new IllegalArgumentException("Interest can only be applied to Savings accounts");
        }
    
        // Skip interest calculation for zero or negative balances
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return account;
        }
    
        // Interest rate is 0.5% (0.005 as decimal)
        BigDecimal interestRate = BigDecimal.valueOf(0.005);
        BigDecimal interestAmount = account.getBalance().multiply(interestRate);
        interestAmount = interestAmount.setScale(2, RoundingMode.HALF_UP);
    
        // Update account balance
        account.setBalance(account.getBalance().add(interestAmount));
    
        // Create and save the transaction for the interest
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(interestAmount);
        transaction.setTransactionType(Transaction.TransactionType.INTEREST);
        transaction.setDescription("Interest credited to Savings account");
        transactionRepository.save(transaction);
    
        // Send notification to the customer
        notificationService.sendEmailNotification(
            account.getCustomer(),
            "Interest Credit Notification",
            "An interest of " + interestAmount + " has been credited to your Savings account."
        );
    
        // Save the updated account and return it
        return accountRepository.save(account);
    }
    
}
