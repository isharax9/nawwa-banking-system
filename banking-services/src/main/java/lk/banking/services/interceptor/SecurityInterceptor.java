package lk.banking.services.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import lk.banking.core.exception.UnauthorizedAccessException;
import lk.banking.core.entity.enums.UserRole; // Import UserRole enum

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger; // Use java.util.logging

public class SecurityInterceptor {

    private static final Logger LOGGER = Logger.getLogger(SecurityInterceptor.class.getName());

    @AroundInvoke
    public Object secure(InvocationContext ctx) throws Exception {
        Method method = ctx.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();

        LOGGER.info("[SECURITY] Intercepting: " + className + "." + methodName + "()");

        // --- CONCEPTUAL AUTHORIZATION LOGIC ---
        // IMPORTANT: This 'simulateCurrentUserRoles' must be replaced by actual
        // integration with your Jakarta Security context to retrieve the real roles
        // of the currently authenticated user in a production environment.
        Set<UserRole> currentUserRoles = simulateCurrentUserRoles(); // Replace with real logic from session/security context

        if (methodName.equals("deleteCustomer") || methodName.equals("deleteAccount") || methodName.equals("removeUser")) {
            if (!(currentUserRoles.contains(UserRole.ADMIN))) { // Only ADMIN can delete
                LOGGER.warning("[SECURITY] DENIED: User is not ADMIN for " + methodName);
                throw new UnauthorizedAccessException("You are not authorized to perform " + methodName + " operations.");
            }
        } else if (methodName.startsWith("create") || methodName.startsWith("update") || methodName.equals("transferFunds") || methodName.equals("processPayment")) {
            // Allow ADMIN, EMPLOYEE, OR CUSTOMER for create/update/transfer/payment operations
            if (!(currentUserRoles.contains(UserRole.ADMIN) || currentUserRoles.contains(UserRole.EMPLOYEE) || currentUserRoles.contains(UserRole.CUSTOMER))) {
                LOGGER.warning("[SECURITY] DENIED: User is not ADMIN, EMPLOYEE, or CUSTOMER for " + methodName);
                throw new UnauthorizedAccessException("You are not authorized to perform " + methodName + " operations.");
            }
            // For methods like createAccount by a CUSTOMER, you might want more fine-grained checks
            // within the EJB itself (e.g., ensure the account is being created for *their* customer ID)
        }
        // Add more specific rules as needed for other methods

        LOGGER.info("[SECURITY] Access granted for: " + className + "." + methodName + "()");
        return ctx.proceed();
    }

    // --- Placeholder for fetching real user roles ---
    // This method is the one you need to replace with actual production security context retrieval.
    // For now, ensure it returns the role of the user you are currently testing with (e.g., CUSTOMER).
    private Set<UserRole> simulateCurrentUserRoles() {
        // !!! IMPORTANT: REPLACE THIS WITH ACTUAL LOGIC TO GET ROLES FROM YOUR SECURITY CONTEXT !!!
        // For example, if you have access to HttpServletRequest (which EJB Interceptors don't directly without a bridge):
        // HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        // LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        // if (loggedInUser != null) {
        //     return loggedInUser.getRoles();
        // }
        // For current testing with a customer user, ensure this returns CUSTOMER.
        return new HashSet<>(Arrays.asList(UserRole.CUSTOMER)); // Temporarily hardcoded for CUSTOMER testing
    }
}