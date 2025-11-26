package com.rivers.core.task;

import com.rivers.core.entity.JobParamReq;
import com.rivers.core.vo.ResultVO;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

public interface BatchTaskHandler {

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    ResultVO<Void> execute(JobParamReq jobParamReq);

    @Recover
    default ResultVO<Void> recover(Exception e, JobParamReq jobParamReq) {
        return ResultVO.fail(e.getMessage());
    }
}
