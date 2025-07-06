package lk.banking.services.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class SecurityInterceptor {
    @AroundInvoke
    public Object secure(InvocationContext ctx) throws Exception {
        // Add security checks here
        System.out.println("[SECURITY] Checking access for: " + ctx.getMethod().getName());
        return ctx.proceed();
    }
}