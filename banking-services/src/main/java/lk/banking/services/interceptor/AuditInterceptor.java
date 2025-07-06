package lk.banking.services.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class AuditInterceptor {
    @AroundInvoke
    public Object audit(InvocationContext ctx) throws Exception {
        System.out.println("[AUDIT] Calling: " + ctx.getMethod().getName());
        return ctx.proceed();
    }
}