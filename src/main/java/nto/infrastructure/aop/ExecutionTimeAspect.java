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

    @Around(
        "@annotation(nto.application.annotations.LogExecutionTime)"
            + " || (within(@org.springframework.web.bind.annotation.RestController *)"
            + " && execution(public * *(..)))"
    )
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object proceed = joinPoint.proceed();
            stopWatch.stop();

            log.info("AOP Metric: Method [{}] executed in {} ms",
                joinPoint.getSignature().toShortString(),
                stopWatch.getTotalTimeMillis());

            return proceed;
        } catch (Throwable ex) {
            stopWatch.stop();

            log.warn("AOP Metric: Method [{}] failed in {} ms: {}",
                joinPoint.getSignature().toShortString(),
                stopWatch.getTotalTimeMillis(),
                ex.getClass().getSimpleName());

            throw ex;
        }
    }
}
