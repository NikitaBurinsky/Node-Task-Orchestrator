package nto.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect // Объявляет класс аспектом
@Component // Регистрирует как Bean, чтобы Spring мог его внедрить
@Slf4j // Lombok для логгера
public class ExecutionTimeAspect {

    // @Around - самый мощный совет (Advice). Позволяет выполнить код ДО и ПОСЛЕ метода, или вообще не вызывать его.
    // Pointcut: любой метод, помеченный @LogExecutionTime
    @Around("@annotation(nto.application.annotations.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Выполняем целевой метод
        Object proceed = joinPoint.proceed();

        stopWatch.stop();

        log.info("Method {} executed in {} ms",
                joinPoint.getSignature().getName(), // Имя метода
                stopWatch.getTotalTimeMillis());    // Время

        return proceed;
    }
}