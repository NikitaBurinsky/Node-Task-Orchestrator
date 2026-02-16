package nto.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    // Advice "Around" оборачивает метод. Аналог `await next()` в Middleware.
    @Around("@annotation(nto.application.annotations.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Вызов целевого метода
        Object proceed = joinPoint.proceed();

        stopWatch.stop();

        log.info("AOP Metric: Method [{}] executed in {} ms",
                joinPoint.getSignature().getName(),
                stopWatch.getTotalTimeMillis());

        return proceed;
    }
}