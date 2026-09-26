package com.rivers.core.task;

import com.rivers.core.entity.JobParamReq;
import com.rivers.core.util.SpringContextUtil;
import com.rivers.core.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 批任务执行入口：接收调度端（timer-batch）派发的任务，异步执行本服务注册的 BatchTaskHandler。
 * 由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配，
 * 非任务执行节点的服务可通过 {@code rivers.batch.executor.enabled=false} 关闭本端点。
 */
@RestController
@RequestMapping("job")
@Slf4j
public class BatchController {

    @PostMapping("execute")
    public ResultVO<Void> execute(@RequestBody JobParamReq jobParamReq) {
        log.info("开始执行批处理任务: {}, 类型: {}", jobParamReq.getTaskName(), jobParamReq.getParams());
        BatchTaskHandler bean = SpringContextUtil.getBean(jobParamReq.getTaskName(), BatchTaskHandler.class);
        CompletableFuture.runAsync(() -> bean.execute(jobParamReq));
        return ResultVO.ok();
    }
}
