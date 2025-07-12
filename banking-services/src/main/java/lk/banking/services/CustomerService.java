package lk.banking.services;

import jakarta.ejb.Local;
import lk.banking.core.dto.CustomerDto;
import lk.banking.core.entity.Customer;

import java.util.List;

@Local
public interface CustomerService {
    Customer createCustomer(CustomerDto customerDto);
    Customer getCustomerById(Long id);
    List<Customer> getAllCustomers();
    Customer updateCustomer(CustomerDto customerDto);
    void deleteCustomer(Long id);
    Customer getCustomerByEmail(String email);
}