package lk.banking.services.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import lk.banking.core.exception.UnauthorizedAccessException;
// You might need to import classes related to your actual security context
// For example:
// import jakarta.security.enterprise.SecurityContext;
// import jakarta.ejb.EJBContext;
// import lk.banking.core.entity.User; // If you fetch User from a custom context

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Interceptor for enforcing security and authorization checks on EJB methods.
 *
 * NOTE: For full security, this should be integrated with Jakarta Security API
 * (SecurityContext, @RolesAllowed) or EJBContext, and a proper authentication
 * mechanism. This provides a placeholder for programmatic checks.
 */
public class SecurityInterceptor {

    @AroundInvoke
    public Object secure(InvocationContext ctx) throws Exception {
        Method method = ctx.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();

        System.out.println("[SECURITY] Intercepting: " + className + "." + methodName + "()");

        // --- CONCEPTUAL AUTHORIZATION LOGIC ---
        // This is where your actual role/permission checks would go.
        // You need a way to get the roles of the *currently authenticated user*.

        // Placeholder for getting current user's roles.
        // In a real Jakarta EE app, you'd use SecurityContext or EJBContext.
        // For example:
        // Set<String> currentUserRoles = getCurrentUserRolesFromSecurityContext();
        Set<String> currentUserRoles = simulateCurrentUserRoles(); // Replace with real logic

        // Example: Define required roles for specific methods/patterns
        if (methodName.equals("deleteCustomer") || methodName.equals("deleteAccount") || methodName.equals("deleteUser")) {
            if (!currentUserRoles.contains("ADMIN")) {
                System.err.println("[SECURITY] DENIED: User is not ADMIN for " + methodName);
                throw new UnauthorizedAccessException("You are not authorized to perform " + methodName + " operations.");
            }
        } else if (methodName.startsWith("create") || methodName.startsWith("update") || methodName.equals("transferFunds")) {
            if (!(currentUserRoles.contains("ADMIN") || currentUserRoles.contains("EMPLOYEE"))) {
                System.err.println("[SECURITY] DENIED: User is not ADMIN or EMPLOYEE for " + methodName);
                throw new UnauthorizedAccessException("You are not authorized to perform " + methodName + " operations.");
            }
        }
        // Add more specific rules as needed.
        // E.g., a "CUSTOMER" might only be allowed to 'get' their own accounts/transactions.
        // This would require checking method parameters (e.g., accountId matches user's owned accounts).
        // This kind of fine-grained access control often becomes complex inside a generic interceptor
        // and is sometimes better handled directly within the service method or by a dedicated
        // authorization component.

        System.out.println("[SECURITY] Access granted for: " + className + "." + methodName + "()");
        return ctx.proceed(); // Proceed with the method invocation
    }

    // --- Placeholder for fetching real user roles ---
    private Set<String> simulateCurrentUserRoles() {
        // !!! IMPORTANT: REPLACE THIS WITH ACTUAL LOGIC TO GET ROLES FROM YOUR SECURITY CONTEXT !!!
        // Example: From Jakarta Security's SecurityContext
        // @Inject private SecurityContext securityContext;
        // ...
        // Set<String> roles = new HashSet<>();
        // if (securityContext.isCallerInRole("ADMIN")) roles.add("ADMIN");
        // if (securityContext.isCallerInRole("EMPLOYEE")) roles.add("EMPLOYEE");
        // if (securityContext.isCallerInRole("CUSTOMER")) roles.add("CUSTOMER");
        // return roles;

        // For now, simulating for testing:
        // return new HashSet<>(Arrays.asList("ADMIN")); // For testing ADMIN access
        // return new HashSet<>(Arrays.asList("EMPLOYEE")); // For testing EMPLOYEE access
        return new HashSet<>(Arrays.asList("CUSTOMER")); // For testing CUSTOMER access
    }
}