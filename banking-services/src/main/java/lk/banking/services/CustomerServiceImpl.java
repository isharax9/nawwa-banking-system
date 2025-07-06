package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.dto.CustomerDto;
import lk.banking.core.entity.Customer;
import lk.banking.core.exception.AccountNotFoundException;

import java.util.List;

@Stateless
public class CustomerServiceImpl implements CustomerService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public Customer createCustomer(CustomerDto customerDto) {
        Customer customer = new Customer(
                customerDto.getName(),
                customerDto.getEmail(),
                customerDto.getAddress(),
                customerDto.getPhoneNumber()
        );
        em.persist(customer);
        return customer;
    }

    @Override
    public Customer getCustomerById(Long id) {
        Customer customer = em.find(Customer.class, id);
        if (customer == null) throw new AccountNotFoundException("Customer not found");
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c", Customer.class).getResultList();
    }

    @Override
    public Customer updateCustomer(CustomerDto customerDto) {
        Customer customer = em.find(Customer.class, customerDto.getId());
        if (customer == null) throw new AccountNotFoundException("Customer not found");
        customer.setName(customerDto.getName());
        customer.setAddress(customerDto.getAddress());
        customer.setEmail(customerDto.getEmail());
        customer.setPhoneNumber(customerDto.getPhoneNumber());
        em.merge(customer);
        return customer;
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer customer = em.find(Customer.class, id);
        if (customer == null) throw new AccountNotFoundException("Customer not found");
        em.remove(customer);
    }
}