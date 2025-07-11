package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException; // Import for specific query results
import lk.banking.core.dto.CustomerDto;
import lk.banking.core.entity.Customer;
import lk.banking.core.exception.CustomerNotFoundException; // Corrected import
import lk.banking.core.exception.ResourceConflictException; // Assuming you'd use this for duplicates
import lk.banking.services.interceptor.AuditInterceptor;
import lk.banking.services.interceptor.PerformanceMonitorInterceptor;
import lk.banking.services.interceptor.SecurityInterceptor;

import java.util.List;

@Stateless
@Interceptors({AuditInterceptor.class, PerformanceMonitorInterceptor.class, SecurityInterceptor.class})
public class CustomerServiceImpl implements CustomerService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public Customer createCustomer(CustomerDto customerDto) {
        // Optional: Check for duplicate email before persisting, to throw ResourceConflictException explicitly
        try {
            em.createQuery("SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", customerDto.getEmail())
                    .getSingleResult();
            throw new ResourceConflictException("Customer with email " + customerDto.getEmail() + " already exists.");
        } catch (NoResultException e) {
            // No customer with this email found, proceed to create
        }

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
        if (customer == null) {
            throw new CustomerNotFoundException("Customer with ID " + id + " not found.");
        }
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c", Customer.class).getResultList();
    }

    @Override
    public Customer updateCustomer(CustomerDto customerDto) {
        Customer customer = em.find(Customer.class, customerDto.getId());
        if (customer == null) {
            throw new CustomerNotFoundException("Customer with ID " + customerDto.getId() + " not found for update.");
        }
        // If email is allowed to be updated, you might need to check for uniqueness
        // just like in createCustomer, especially if it's a critical identifier.
        // For simplicity, we're assuming the update won't conflict with another existing email.
        customer.setName(customerDto.getName());
        customer.setAddress(customerDto.getAddress());
        customer.setEmail(customerDto.getEmail());
        customer.setPhoneNumber(customerDto.getPhoneNumber());
        // em.merge(customer); // Not strictly necessary if 'customer' is already a managed entity
        return customer; // The managed entity 'customer' will automatically update upon transaction commit
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer customer = em.find(Customer.class, id); // Find to ensure it exists and is managed
        if (customer == null) {
            throw new CustomerNotFoundException("Customer with ID " + id + " not found for deletion.");
        }
        em.remove(customer);
    }
}