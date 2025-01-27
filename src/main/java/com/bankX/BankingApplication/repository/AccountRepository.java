package com.bankX.BankingApplication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bankX.BankingApplication.model.Account;
import com.bankX.BankingApplication.model.Account.AccountType;
import com.bankX.BankingApplication.model.Customer;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
     Account findByCustomerAndAccountType(Customer customer, AccountType accountType);
}