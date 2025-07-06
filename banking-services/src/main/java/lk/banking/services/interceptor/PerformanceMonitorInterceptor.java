package lk.banking.services.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class PerformanceMonitorInterceptor {
    @AroundInvoke
    public Object monitor(InvocationContext ctx) throws Exception {
        long start = System.currentTimeMillis();
        try {
            return ctx.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[PERFORMANCE] " + ctx.getMethod().getName() + " took " + elapsed + "ms");
        }
    }
}