package com.bankX.BankingApplication.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByAccountAndAmountAndDescription(Account account, BigDecimal amount, String description);
}