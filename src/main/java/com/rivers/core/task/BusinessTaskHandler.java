package com.rivers.core.task;

import com.rivers.core.entity.JobParamReq;
import com.rivers.core.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BusinessTaskHandler implements BatchTaskHandler {

    protected abstract ResultVO<Void> doExecute(JobParamReq jobParamReq);

    @Override
    public ResultVO<Void> execute(JobParamReq jobParamReq) {
        // 前置处理
        log.info("开始执行批处理任务: {}, 类型: {}", jobParamReq.getServerName(), jobParamReq.getParams());
        // 执行业务逻辑
        ResultVO<Void> result = doExecute(jobParamReq);
        log.info("批处理任务完成");
        return result;
    }

    @Override
    public ResultVO<Void> recover(Exception e, JobParamReq jobParamReq) {
        return ResultVO.fail(e.getMessage());
    }
}
